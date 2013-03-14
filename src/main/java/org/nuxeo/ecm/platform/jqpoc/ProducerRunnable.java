package org.nuxeo.ecm.platform.jqpoc;

public class ProducerRunnable implements Runnable {
    private final long produceUntil;

    private final String prefix;

    private String queueName;

    private final int bucket;

    ProducerRunnable(String prefix, String queueName, long produceUntil, int bucket) {
        this.produceUntil = produceUntil;
        this.prefix = prefix;
        this.queueName = queueName;
        this.bucket = bucket;
    }

    @Override
    public void run() {
        JobQueue queue = new JobQueue(queueName, 10);
        String[] ids = new String[bucket];
        try {
            for (int i = 0; i < produceUntil;) {
                for (int j=0; j< bucket; j++) {
                    ids[j] = prefix + ':' + i++;
                }
                queue.addJobIds(ids);
            }
        } finally {
            queue.close();
        }
    }
}