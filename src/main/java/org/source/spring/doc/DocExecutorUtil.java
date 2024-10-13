package org.source.spring.doc;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class DocExecutorUtil {
    public static final ExecutorService DOC_TASK = TtlExecutors.getTtlExecutorService(new ThreadPoolExecutor(1, 4,
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));
}
