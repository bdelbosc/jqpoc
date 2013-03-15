package org.nuxeo.ecm.platform.jqpoc;

public class ProducerRunnable implements Runnable {
    private final long produceUntil;

    private final String prefix;

    private final String queueName;

    private final int bucket;

    private final String host;

    private final int port;

    ProducerRunnable(final String host, final int port, final String prefix,
            final String queueName, final long produceUntil, final int bucket) {
        this.host = host;
        this.port = port;
        this.produceUntil = produceUntil;
        this.prefix = prefix;
        this.queueName = queueName;
        this.bucket = bucket;
    }

    @Override
    public void run() {
        JobQueue queue = new JobQueue(host, port, queueName, 10);
        String[] ids = new String[bucket];
        try {
            for (int i = 0; i < produceUntil;) {
                for (int j = 0; j < bucket; j++) {
                    ids[j] = prefix + ':' + i++;
                }
                queue.addJobIds(ids);
            }
        } finally {
            queue.disconnect();
        }
    }
}