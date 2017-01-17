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
    private static final Logger LOGGER = LoggerFactory.getLogger(ForciblyStoppableTestWorker.class);
    private static final int SHUTDOWN_TIMEOUT = 60;

    public ForciblyStoppableTestWorker(WorkerTestClassProcessorFactory factory)
    {
        super(factory);
    }

    @Override public void stop()
    {
        new Timer().schedule(new TimerTask()
        {
            @Override public void run()
            {
                LOGGER.warn("Worker process did not shutdown gracefully within {}s, forcing it now", SHUTDOWN_TIMEOUT);
                Runtime.getRuntime().halt(-100);
            }
        }, TimeUnit.SECONDS.toMillis(SHUTDOWN_TIMEOUT));
        super.stop();
    }
}
