package org.nuxeo.ecm.platform.jqpoc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;

/**
 * Job Queue
 *
 */
public class JobQueue {

    private static final Log log = LogFactory.getLog(JobQueue.class);

    private final Jedis jedis;

    private final String name;

    private final long timeout;

    private long size = -1;

    private long maxSize = -1;

    public JobQueue(final String name, long timeout) {
        this.name = name;
        this.timeout = timeout;
        log.debug("Creating new JobQueue for: " + name);
        jedis = new Jedis("localhost");

        Metrics.defaultRegistry().newGauge(JobQueue.class, "pending-" + name,
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return jedis.llen(name);
                    }
                });
    }

    public long addJobId(String jid) {
        size = jedis.lpush(name, jid);
        maxSize = Math.max(size, maxSize);
        return size;
    }

    public JobRef getJob() {
        final long now = (long) (System.currentTimeMillis() / 1000);
        final String timestamp = String.format("%d", now);
        String key = (String) jedis.eval(
                "local v = redis.call('RPOP',ARGV[1]) " + //
                        "if not v then return v end " + //
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
        return jedis.lrem(name, 1, key);
    }

    public long flush() {
        size = 0;
        return jedis.del(name);
    }

    public long getSize() {
        return size;
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
