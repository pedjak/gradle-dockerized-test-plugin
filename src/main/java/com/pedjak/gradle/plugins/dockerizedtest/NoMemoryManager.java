package com.pedjak.gradle.plugins.dockerizedtest;

import org.gradle.process.internal.health.memory.JvmMemoryStatusListener;
import org.gradle.process.internal.health.memory.MemoryHolder;
import org.gradle.process.internal.health.memory.MemoryManager;
import org.gradle.process.internal.health.memory.OsMemoryStatusListener;

public class NoMemoryManager implements MemoryManager
{
    @Override public void addListener(JvmMemoryStatusListener jvmMemoryStatusListener)
    {

    }

    @Override public void addListener(OsMemoryStatusListener osMemoryStatusListener)
    {

    }

    @Override public void removeListener(JvmMemoryStatusListener jvmMemoryStatusListener)
    {

    }

    @Override public void removeListener(OsMemoryStatusListener osMemoryStatusListener)
    {

    }

    @Override public void addMemoryHolder(MemoryHolder memoryHolder)
    {

    }

    @Override public void removeMemoryHolder(MemoryHolder memoryHolder)
    {

    }

    @Override public void requestFreeMemory(long l)
    {

    }
}
