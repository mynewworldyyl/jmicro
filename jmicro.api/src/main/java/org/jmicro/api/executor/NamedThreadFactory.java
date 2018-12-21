package org.jmicro.api.executor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    protected static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    protected final AtomicInteger mThreadNum = new AtomicInteger(1);

    protected final String mPrefix;

    protected final ThreadGroup mGroup;

    public NamedThreadFactory(String subfix) {
       mPrefix = "JMicro-TP-" + subfix;
       SecurityManager s = System.getSecurityManager();
       mGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = mPrefix + mThreadNum.getAndIncrement();
        Thread ret = new Thread(mGroup, runnable, name, 0);
        ret.setDaemon(true);
        return ret;
    }

    public ThreadGroup getThreadGroup() {
        return mGroup;
    }
}
