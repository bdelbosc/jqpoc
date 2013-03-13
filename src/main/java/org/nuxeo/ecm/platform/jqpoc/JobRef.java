package org.nuxeo.ecm.platform.jqpoc;

public class JobRef {
    final private String key;

    final private String jid;

    final private String stamp;

    final private JobState state;

    public JobRef(String key, String jid, String stamp, JobState state) {
        this.key = key;
        this.jid = jid;
        this.stamp = stamp;
        this.state = state;
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

    public JobState getState() {
        return state;
    }

    @Override
    public String toString() {
        return String.format("key=%s, id=%s stamp=%s state=%s", key, jid,
                stamp, state.toString());
    }

}
