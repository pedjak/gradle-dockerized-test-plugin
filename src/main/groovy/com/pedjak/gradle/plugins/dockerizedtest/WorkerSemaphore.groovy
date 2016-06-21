package com.pedjak.gradle.plugins.dockerizedtest

import org.gradle.api.Project

interface WorkerSemaphore
{

    void acquire()

    void release()

    void applyTo(Project project)
}