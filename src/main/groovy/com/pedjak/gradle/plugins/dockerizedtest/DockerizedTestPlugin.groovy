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

package com.pedjak.gradle.plugins.dockerizedtest

import org.gradle.StartParameter
import org.gradle.api.*
import org.gradle.internal.concurrent.ExecutorFactory
import org.gradle.messaging.actor.ActorFactory
import org.gradle.api.internal.*
import org.gradle.api.internal.file.*
import org.gradle.internal.id.*
import org.gradle.api.tasks.testing.Test
import org.gradle.api.internal.tasks.testing.detection.*
import org.gradle.messaging.remote.internal.IncomingConnector
import org.gradle.messaging.remote.internal.MessagingServices
import org.apache.commons.lang3.SystemUtils
import org.apache.maven.artifact.versioning.ComparableVersion
import org.gradle.process.internal.child.EncodedStream
import org.gradle.util.GUtil

import javax.inject.Inject

class DockerizedTestPlugin implements Plugin<Project> {

    def actorFactory
    def startParameter
    def incommingConnector
    def executorFactory
    def resolver
    def currentUser

    @Inject
    DockerizedTestPlugin(
            StartParameter startParameter,
            ExecutorFactory executorFactory,
            MessagingServices messagingServices, FileResolver resolver, ActorFactory actorFactory) {
        this.actorFactory = actorFactory
        this.incommingConnector = messagingServices.get(IncomingConnector)
        this.startParameter = startParameter
        this.executorFactory = executorFactory
        this.resolver = resolver
        this.currentUser = SystemUtils.IS_OS_WINDOWS ? "0" : "id -u".execute().text.trim()
    }

    void configureTest(project, test, attachStdInContent) {
        def ext = test.extensions.create("docker", DockerizedTestExtension, [] as Object[])
        ext.volumes = [ "$startParameter.gradleUserHomeDir": "$startParameter.gradleUserHomeDir",
                        "$project.projectDir":"$project.projectDir"]
        ext.user = currentUser
        test.doFirst {
            def extension = test.extensions.docker
            if (extension?.image) {
                test.testExecuter = new DefaultTestExecuter(newProcessBuilderFactory(extension, attachStdInContent, project.gradle.services.get(ClassPathRegistry)), actorFactory);
            }
        }
    }

    void apply(Project project) {


        boolean preGradle2_4 = new ComparableVersion(project.gradle.gradleVersion).compareTo(new ComparableVersion('2.4')) < 0
        def attachStdInContent = preGradle2_4 ? { workerFactory, javaCommand ->
            def bytes = new ByteArrayOutputStream();
            def encoded = new EncodedStream.EncodedOutput(bytes);
            GUtil.serialize(workerFactory.create(), encoded);
            def stdinContent = new ByteArrayInputStream(bytes.toByteArray());
            javaCommand.setStandardInput(stdinContent);
        } : { workerFactory, javaCommand -> /* no-op */ }

        project.tasks.withType(Test).each { test -> configureTest(project, test, attachStdInContent) }
        project.tasks.whenTaskAdded { task ->
            if (task instanceof Test) configureTest(project, task, attachStdInContent)
        }
    }

    def newProcessBuilderFactory(extension, attachStdInContent, classPathRegistry) {
        new DockerizedWorkerProcessFactory(startParameter.logLevel, incommingConnector, executorFactory, classPathRegistry, resolver, extension, new LongIdGenerator(), attachStdInContent)
    }
}