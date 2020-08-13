package com.github.cszxyang.devtools.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class VisualThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private final Logger logger = LoggerFactory.getLogger(VisualThreadPoolTaskExecutor.class);

    private void showThreadPoolInfo(String prefix) {
        ThreadPoolExecutor executor = getThreadPoolExecutor();
        logger.info("{}, {}, taskCount: {}, completedTaskCount: {}, activeCount: {}, queueSize: {}",
                this.getThreadNamePrefix(),
                prefix,
                executor.getTaskCount(),
                executor.getCompletedTaskCount(),
                executor.getActiveCount(),
                executor.getQueue().size());
    }

    public VisualThreadPoolTaskExecutor() {
        super();
    }

    @Override
    public void execute(Runnable task) {
        showThreadPoolInfo("do execute");
        super.execute(task);
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        showThreadPoolInfo("do execute");
        super.execute(task, startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        showThreadPoolInfo("do execute");
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        showThreadPoolInfo("do submit");
        return super.submit(task);
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        showThreadPoolInfo("do submitListenable");
        return super.submitListenable(task);
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        showThreadPoolInfo("do submitListenable");
        return super.submitListenable(task);
    }
}
