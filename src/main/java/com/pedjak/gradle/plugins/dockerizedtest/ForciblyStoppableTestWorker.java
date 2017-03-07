package com.pedjak.gradle.plugins.dockerizedtest;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory;
import org.gradle.api.internal.tasks.testing.worker.TestWorker;

public class ForciblyStoppableTestWorker extends TestWorker
{
    private static final int SHUTDOWN_TIMEOUT = 60; // secs

    public ForciblyStoppableTestWorker(WorkerTestClassProcessorFactory factory)
    {
        super(factory);
    }

    @Override public void stop()
    {
        new Timer(true).schedule(new TimerTask()
        {
            @Override public void run()
            {
                System.err.println("Worker process did not shutdown gracefully within "+SHUTDOWN_TIMEOUT+"s, forcing it now");
                Runtime.getRuntime().halt(-100);
            }
        }, TimeUnit.SECONDS.toMillis(SHUTDOWN_TIMEOUT));
        super.stop();
    }
}
