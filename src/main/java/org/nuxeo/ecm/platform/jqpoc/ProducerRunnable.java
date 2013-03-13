package org.nuxeo.ecm.platform.jqpoc;

public class ProducerRunnable implements Runnable {
    private final long produceUntil;

    private final String prefix;

    private String queueName;

    ProducerRunnable(String prefix, String queueName, long produceUntil) {
        this.produceUntil = produceUntil;
        this.prefix = prefix;
        this.queueName = queueName;
    }

    @Override
    public void run() {
        JobQueue queue = new JobQueue(queueName, 10);
        try {
            for (long i = 1; i < produceUntil; i++) {
                queue.addJobId(prefix + ':' + i);
            }
        } finally {
            queue.close();
        }
    }
}