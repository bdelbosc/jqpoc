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

    private static final int NTHREDS = 1;

    private static final int TIMEOUT = 10;

    private static final String QUEUE_NAME = "q";

    private static final String PREFIX_JID = "t";

    protected final static Timer producerTimer = Metrics.defaultRegistry().newTimer(
            Producer.class, "producer", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    private static final int BUCKET = 500;

    public static void main(String[] args) {

        GraphiteReporter.enable(1, TimeUnit.SECONDS, "localhost", 2030,
                "java.strix.");

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
        log.info("Starting " + NTHREDS + " threads of producers");

        JobQueue q = new JobQueue(QUEUE_NAME, TIMEOUT);
        q.flush();
        TimerContext clock = producerTimer.time();
        for (int i = 0; i < NTHREDS; i++) {
            Runnable worker = new ProducerRunnable(PREFIX_JID
                    + Integer.valueOf(i).toString(), QUEUE_NAME,
                    (long) (NJOBS / NTHREDS), BUCKET);
            executor.execute(worker);
        }
        log.info("Producer running");
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        long elapsed = clock.stop();
        log.info(elapsed);
        double rate = (NJOBS / (double) elapsed) * 1000000000;
        log.info(String.format(
                "All producer finished, queue size=%d rate=%.0f j/s elapsed=%.3f s",
                q.getPendingJobCount(), rate, elapsed / (double) 1000000000));
        q.close();
    }

}
