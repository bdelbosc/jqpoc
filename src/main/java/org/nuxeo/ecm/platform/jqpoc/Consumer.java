package org.nuxeo.ecm.platform.jqpoc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.yammer.metrics.graphite.GraphiteReporter;

public class Consumer {
    private static final Log log = LogFactory.getLog(Consumer.class);

    private static final int NTHREDS = 20;

    private static final String QUEUE_NAME = "q";

    private static final long DELAY_MS = 0;

    public static void main(String[] args) {
        GraphiteReporter.enable(1, TimeUnit.SECONDS, "localhost",
                2030, "java.strix.");

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

        JobQueue queue = new JobQueue(QUEUE_NAME, 10);
        log.info("Starting " + NTHREDS + " threads of consumers, pending job: "
                + queue.getSize());

        for (int i = 0; i < NTHREDS; i++) {
            Runnable worker = new ConsumerRunnable(QUEUE_NAME, DELAY_MS);
            executor.execute(worker);
        }
        log.info("Consumers running");
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        log.info("All consumers finished, queue size=" + queue.getSize());

    }

}
