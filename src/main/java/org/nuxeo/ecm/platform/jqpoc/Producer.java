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

public class Producer {
    private static final Log log = LogFactory.getLog(Producer.class);

    private static final int NJOBS = 1000000;

    private static final int NTHREDS = 10;

    private static final int TIMEOUT = 10;

    private static final String QUEUE_NAME = "q";

    private static final String PREFIX_JID = "t";

    protected final static Timer producerTimer = Metrics.defaultRegistry().newTimer(
            Producer.class, "producer",
            TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    public static void main(String[] args) {

        GraphiteReporter.enable(1, TimeUnit.SECONDS, "localhost",
               2030, "java.strix.");

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
        log.info("Starting " + NTHREDS + " threads of producers");
        // raz the queue
        JobQueue q = new JobQueue(QUEUE_NAME, TIMEOUT);
        q.flush();
        TimerContext clock = producerTimer.time();
        for (int i = 0; i < NTHREDS; i++) {
            Runnable worker = new ProducerRunnable(PREFIX_JID
                    + Integer.valueOf(i).toString(), QUEUE_NAME, (long) (NJOBS / NTHREDS));
            executor.execute(worker);
        }
        log.info("Producer running");
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        clock.stop();
        log.info("All producer finished, queue size=" + q.getSize());
        q.close();
    }

}
