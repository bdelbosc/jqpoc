package org.nuxeo.ecm.platform.jqpoc;

public class ConsumerRunnable implements Runnable {

    private final String queueName;

    private final Long delayMs;

    private final String host;

    private final int port;

    ConsumerRunnable(final String host, final int port, final String queueName,
            final long delayMs) {
        this.host = host;
        this.port = port;
        this.queueName = queueName;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        JobQueue queue = new JobQueue(host, port, queueName, 10);
        try {
            JobRef job;
            do {
                job = queue.getJob();
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    return;
                }
                if (job.getState() == JobState.READY) {
                    queue.jobDone(job.getKey());
                }
            } while (job.getState() != JobState.NONE);
        } finally {
            queue.disconnect();
        }
    }
}