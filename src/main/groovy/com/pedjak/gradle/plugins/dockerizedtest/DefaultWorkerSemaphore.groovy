package com.pedjak.gradle.plugins.dockerizedtest

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

import java.util.concurrent.Semaphore

class DefaultWorkerSemaphore implements WorkerSemaphore
{
    private int maxWorkers = Integer.MAX_VALUE
    private Semaphore semaphore
    private logger

    @Override
    void acquire()
    {
        semaphore().acquire()
    }

    @Override
    void release()
    {
        semaphore().release()
    }

    @Override
    void applyTo(Project project)
    {
        if (!logger) {
            logger = project.logger
        }

        project.afterEvaluate {
            maxWorkers = project.tasks.withType(Test).findAll {
                it.extensions.docker?.image != null
            }.collect {
                def v = it.maxParallelForks
                it.maxParallelForks = 10000
                v
            }.min() ?: 1
        }
    }

    private synchronized setMaxWorkers(int num) {
        if (this.@maxWorkers > num) {
            this.@maxWorkers = num
        }
    }

    private synchronized Semaphore semaphore() {
        if (semaphore == null) {
            semaphore = new Semaphore(maxWorkers)
            logger.info("Do not allow more than {} test workers", maxWorkers)
        }
        semaphore
    }
}
