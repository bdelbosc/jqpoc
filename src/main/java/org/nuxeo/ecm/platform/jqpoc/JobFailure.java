package org.nuxeo.ecm.platform.jqpoc;

/**
 * Container for a Job completed in failure
 *
 */
public class JobFailure {
    final private String key;

    final private String jid;

    final private String stamp;

    final private String message;

    public JobFailure(String key, String jid, String stamp, String message) {
        this.key = key;
        this.jid = jid;
        this.stamp = stamp;
        this.message = message;
    }

    public String getId() {
        return jid;
    }

    public String getKey() {
        return key;
    }

    public String getStamp() {
        return stamp;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("key=%s, id=%s stamp=%s error=%s", key, jid,
                stamp, message);
    }

}
