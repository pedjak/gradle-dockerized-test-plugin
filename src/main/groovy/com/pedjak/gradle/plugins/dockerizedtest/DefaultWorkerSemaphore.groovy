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
        logger.debug("Semaphore acquired, available: {}/{}", semaphore().availablePermits(), maxWorkers)
    }

    @Override
    void release()
    {
        semaphore().release()
        logger.debug("Semaphore released, available: {}/{}", semaphore().availablePermits(), maxWorkers)
    }

    @Override
    synchronized void applyTo(Project project)
    {
        if (semaphore) return
        if (!logger) {
            logger = project.logger
        }

        maxWorkers = project.tasks.withType(Test).findAll {
            it.extensions.docker?.image != null
        }.collect {
            def v = it.maxParallelForks
            it.maxParallelForks = 10000
            v
        }.min() ?: 1
        semaphore()
    }

    private synchronized setMaxWorkers(int num) {
        if (this.@maxWorkers > num) {
            this.@maxWorkers = num
        }
    }

    private synchronized Semaphore semaphore() {
        if (semaphore == null) {
            semaphore = new Semaphore(maxWorkers)
            logger.lifecycle("Do not allow more than {} test workers", maxWorkers)
        }
        semaphore
    }
}
