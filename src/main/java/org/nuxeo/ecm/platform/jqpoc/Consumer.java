package org.nuxeo.ecm.platform.jqpoc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.graphite.GraphiteReporter;

public class Consumer {
    private static final Log log = LogFactory.getLog(Consumer.class);

    private static final int NTHREDS = 6;

    private static final String QUEUE_NAME = "q";

    private static final long DELAY_MS = 0;

    protected final static Timer consumerTimer = Metrics.defaultRegistry().newTimer(
            Producer.class, "consumer", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    public static void main(String[] args) {
        GraphiteReporter.enable(1, TimeUnit.SECONDS, "localhost", 2030,
                "java.strix.");

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

        JobQueue queue = new JobQueue(QUEUE_NAME, 10);
        long size = queue.getPendingJobCount();
        log.info("Starting " + NTHREDS + " threads of consumers, pending job: "
                + size);
        TimerContext clock = consumerTimer.time();

        for (int i = 0; i < NTHREDS; i++) {
            Runnable worker = new ConsumerRunnable(QUEUE_NAME, DELAY_MS);
            executor.execute(worker);
        }
        log.info("Consumers running");
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        long elapsed = clock.stop();
                double rate = (size / (double) elapsed) * 1000000000;
        log.info(String.format(
                "All consumers finished, queue size=%d rate=%.0f j/s elapsed=%.3f s",
                queue.getPendingJobCount(), rate, elapsed / (double) 1000000000));
    }

}
