package org.nuxeo.ecm.platform.jqpoc;

public class ConsumerRunnable implements Runnable {

    private final String queueName;

    private final Long delayMs;

    ConsumerRunnable(String queueName, long delayMs) {
        this.queueName = queueName;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        JobQueue queue = new JobQueue(queueName, 10);
        try {
            JobRef job;
            do {
                job = queue.getJob();
                // System.out.println(job);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    return;
                }
                if (job.getState() == JobState.READY) {
                    queue.completedJob(job.getKey());
                }
            } while (job.getState() != JobState.NONE);
        } finally {
            queue.close();
        }
    }
}