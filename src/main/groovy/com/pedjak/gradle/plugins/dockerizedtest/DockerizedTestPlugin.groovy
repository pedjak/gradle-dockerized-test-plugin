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

import javax.inject.Inject

class DockerizedTestPlugin implements Plugin<Project> {

    def actorFactory
    def startParameter
    def incommingConnector
    def executorFactory
    def classPathRegistry
    def resolver

    @Inject
    DockerizedTestPlugin(
            StartParameter startParameter,
            ExecutorFactory executorFactory,
            MessagingServices messagingServices,
            ClassPathRegistry classPathRegistry, FileResolver resolver, ActorFactory actorFactory) {
        this.actorFactory = actorFactory
        this.incommingConnector = messagingServices.get(IncomingConnector)
        this.startParameter = startParameter
        this.executorFactory = executorFactory
        this.classPathRegistry = classPathRegistry
        this.resolver = resolver
    }

    void apply(Project project) {

        project.tasks.withType(Test).each { test ->
            test.extensions.create("docker", DockerizedTestExtension, [] as Object[])
        }

        project.afterEvaluate {
            project.tasks.withType(Test).each { test ->
                def extension = test.extensions.docker
                if (extension.image) {
                    test.testExecuter = new DefaultTestExecuter(newProcessBuilderFactory(extension), actorFactory);
                }
            }

        }

    }

    def newProcessBuilderFactory(extension) {
        new DockerizedWorkerProcessFactory(startParameter.logLevel, incommingConnector, executorFactory, classPathRegistry, resolver, extension, new LongIdGenerator())
    }
}