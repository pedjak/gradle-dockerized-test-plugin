/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedjak.gradle.plugins.dockerizedtest;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.internal.ClassPathRegistry;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.logging.LogLevel;
import org.gradle.internal.Factory;
import org.gradle.internal.classloader.ClasspathUtil;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.id.IdGenerator;
import org.gradle.messaging.remote.Address;
import org.gradle.messaging.remote.ConnectionAcceptor;
import org.gradle.messaging.remote.ObjectConnection;
import org.gradle.messaging.remote.internal.ConnectCompletion;
import org.gradle.messaging.remote.internal.IncomingConnector;
import org.gradle.messaging.remote.internal.hub.MessageHubBackedObjectConnection;
import org.gradle.process.internal.child.ApplicationClassesInIsolatedClassLoaderWorkerFactory;
import org.gradle.process.internal.child.ApplicationClassesInSystemClassLoaderWorkerFactory;
import org.gradle.process.internal.child.EncodedStream;
import org.gradle.process.internal.child.WorkerFactory;
import org.gradle.process.internal.*;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DockerizedWorkerProcessFactory implements Factory<WorkerProcessBuilder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerizedWorkerProcessFactory.class);
    private final LogLevel workerLogLevel;
    private final IncomingConnector connector;
    private final ClassPathRegistry classPathRegistry;
    private final FileResolver resolver;
    private final IdGenerator<?> idGenerator;
    private final ExecutorFactory executorFactory;
    private final DockerizedTestExtension extension;
    private final Closure attachStdInContent;

    public DockerizedWorkerProcessFactory(LogLevel workerLogLevel, IncomingConnector connector, ExecutorFactory executorFactory,
                                       ClassPathRegistry classPathRegistry, FileResolver resolver, DockerizedTestExtension extension,
                                       IdGenerator<?> idGenerator, Closure attachStdInContent) {
        this.workerLogLevel = workerLogLevel;
        this.connector = connector;
        this.executorFactory = executorFactory;
        this.classPathRegistry = classPathRegistry;
        this.resolver = resolver;
        this.idGenerator = idGenerator;
        this.extension = extension;
        this.attachStdInContent = attachStdInContent;
    }

    public WorkerProcessBuilder create() {
        return new DockerizedWorkerProcessBuilder();
    }

    private class ConnectEventAction implements Action<ConnectCompletion> {
        private final Action<ObjectConnection> action;

        public ConnectEventAction(Action<ObjectConnection> action) {
            this.action = action;
        }

        public void execute(ConnectCompletion completion) {
            action.execute(new MessageHubBackedObjectConnection(executorFactory, completion));
        }
    }
    private class DockerizedWorkerProcessBuilder extends WorkerProcessBuilder {
        private DockerizedJavaExecHandleBuilder execHandleBuilder;

        public DockerizedWorkerProcessBuilder() {
            super(resolver);
            execHandleBuilder = new DockerizedJavaExecHandleBuilder(extension, resolver);
            setLogLevel(workerLogLevel);
        }

        @Override
        public JavaExecHandleBuilder getJavaCommand() {
            return execHandleBuilder;
        }

        @Override
        public WorkerProcess build() {
            if (getWorker() == null) {
                throw new IllegalStateException("No worker action specified for this worker process.");
            }

            final DefaultWorkerProcess workerProcess = new DefaultWorkerProcess(120, TimeUnit.SECONDS);
            ConnectionAcceptor acceptor = connector.accept(new ConnectEventAction(new Action<ObjectConnection>() {
                public void execute(ObjectConnection connection) {
                    workerProcess.onConnect(connection);
                }
            }), true);
            workerProcess.startAccepting(acceptor);
            Address localAddress = acceptor.getAddress();
            // Build configuration for GradleWorkerMain
            List<URL> implementationClassPath = ClasspathUtil.getClasspath(getWorker().getClass().getClassLoader());
            Object id = idGenerator.generateId();
            String displayName = getBaseName() + " " + id;

            WorkerFactory workerFactory;
            if (isLoadApplicationInSystemClassLoader()) {
                workerFactory = new ApplicationClassesInSystemClassLoaderWorkerFactory(id, displayName, this,
                        implementationClassPath, localAddress, classPathRegistry);
            } else {
                workerFactory = new ApplicationClassesInIsolatedClassLoaderWorkerFactory(id, displayName, this,
                        implementationClassPath, localAddress, classPathRegistry);
            }

            LOGGER.debug("Creating {}", displayName);
            LOGGER.debug("Using application classpath {}", getApplicationClasspath());
            LOGGER.debug("Using implementation classpath {}", implementationClassPath);

            JavaExecHandleBuilder javaCommand = getJavaCommand();
            attachStdInContent.call(workerFactory, javaCommand);
            workerFactory.prepareJavaCommand(javaCommand);
            javaCommand.setDisplayName(displayName);
            javaCommand.args("'" + displayName + "'");
            ExecHandle execHandle = javaCommand.build();

            workerProcess.setExecHandle(execHandle);

            return workerProcess;
        }

    }
}
