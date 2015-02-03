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

import org.gradle.api.internal.file.FileResolver
import org.gradle.process.internal.*
import org.gradle.process.internal.streams.StreamsForwarder
import org.gradle.process.internal.streams.StreamsHandler

class DockerizedJavaExecHandleBuilder extends JavaExecHandleBuilder {

    def streamsHandler
    private DockerizedTestExtension extension

    DockerizedJavaExecHandleBuilder(DockerizedTestExtension extension, FileResolver fileResolver) {
        super(fileResolver)
        this.extension = extension
    }

    def StreamsHandler getStreamsHandler() {
        StreamsHandler effectiveHandler;
        if (this.streamsHandler != null) {
            effectiveHandler = this.streamsHandler;
        } else {
            boolean shouldReadErrorStream = !redirectErrorStream;
            effectiveHandler = new StreamsForwarder(standardOutput, errorOutput, standardInput, shouldReadErrorStream);
        }
        return effectiveHandler;
    }

    ExecHandle build() {

        def userHome = System.properties['user.home']
        StreamsHandler effectiveHandler = getStreamsHandler();
        def args = ['run',
                    '--rm=true',
                    '-w',
                    workingDir.absolutePath] +
                extension.volumes.collect { p1, p2 ->
                    ['-v', "$p1:$p2".toString()]
                }.flatten() +
                (actualEnvironment.collect { k, v ->
                    ['-e', "$k=$v".toString()]
                }).flatten() + [
                '-i']
        if (extension.user) {
            args += ["-u", extension.user]
        }
        args += [extension.image,
                 'java'
        ] + allArguments
        if (extension.argsInspect != null) args = extension.argsInspect(args)

        return new DefaultExecHandle(getDisplayName(), getWorkingDir(), 'docker', args, getActualEnvironment(),
                effectiveHandler, listeners, redirectErrorStream, timeoutMillis, daemon);

    }

    def timeoutMillis = Integer.MAX_VALUE
    @Override
    AbstractExecHandleBuilder setTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis
        return super.setTimeout(timeoutMillis)
    }

    boolean redirectErrorStream
    @Override
    AbstractExecHandleBuilder redirectErrorStream() {
        redirectErrorStream = true
        return super.redirectErrorStream()
    }

    def listeners = []
    @Override
    AbstractExecHandleBuilder listener(ExecHandleListener listener) {
        listeners << listener
        return super.listener(listener)
    }
}
