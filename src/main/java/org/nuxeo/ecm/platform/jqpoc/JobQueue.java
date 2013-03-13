package org.nuxeo.ecm.platform.jqpoc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Job Queue
 *
 */
public class JobQueue {

    private static final Log log = LogFactory.getLog(JobQueue.class);

    final private Jedis jedis;

    final private String name;

    final private long timeout;

    public JobQueue(final String name, long timeout) {
        this.name = name;
        this.timeout = timeout;
        log.debug("Creating new JobQueue for: " + name);
        jedis = new Jedis("localhost");
    }

    public long addJobId(String jid) {
        long count = jedis.lpush(name, jid);
        return count;
    }

    public JobRef getJob() {
        final long now = (long) (System.currentTimeMillis() / 1000);
        final String timestamp = String.format("%d", now);
        String key = (String) jedis.eval(
                "local v = redis.call('RPOP',ARGV[1]) " + //
                        "local nv = v " + //
                        "if not string.find(v,'*') then" + //
                        " v = v..'*'..ARGV[2] end " + //
                        "redis.call('LPUSH',ARGV[1],v)" + //
                        "return nv", 0, name, timestamp);
        final String jid;
        final String stamp;
        final JobState state;
        if (key == null) {
            // job less
            jid = stamp = null;
            state = JobState.NONE;
        } else {
            final int pos = key.indexOf('*');
            if (pos <= 0) {
                jid = key;
                stamp = timestamp;
                key = jid + "*" + stamp;
                state = JobState.READY;
            } else {
                jid = key.substring(0, pos);
                stamp = key.substring(pos + 1);
                if (now - Long.valueOf(stamp) < timeout) {
                    state = JobState.PROCESSING;
                } else {
                    state = JobState.TIMEDOUT;
                }
            }
        }
        return new JobRef(key, jid, stamp, state);

    }

    public long completedJob(final String key) {
        log.info("removeing " + key);
        return jedis.lrem(name, 1, key);
    }

    public long flush() {
        return jedis.del(name);
    }

    public void close() {
        log.debug("Disconnecting from JobQueue: " + name);
        if (jedis != null) {
            try {
                jedis.disconnect();
            } catch (JedisException e) {
                log.error("Failed to disconnect jedis", e);
            }
        }
    }

}
