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

    public JobQueue(final String name) {
        this.name = name;
        log.debug("Creating new JobQueue for " + name);
        jedis = new Jedis("localhost");

    }

    public long addJob(String jid) {
        long count = jedis.lpush(name, jid);
        return count;
    }

    public String getJob() {
        final String timestamp = String.format("%.0f",
                ((double) System.currentTimeMillis()) / 1000);
        final String jid = (String) jedis.eval(
                "local v = redis.call('RPOP',ARGV[1]) if not string.find(v,'*') then v = v..'*'..ARGV[2] end redis.call('LPUSH',ARGV[1],v) return v",
                0, name, timestamp);
        return jid;
    }

    public long completedJob(final String jid) {
        return jedis.lrem(name, 1, jid);
    }

    public long flush() {
        return jedis.del(name);
    }

    public void close() {
        log.debug("Disconnecting from JobQueue" + name);
        if (jedis != null) {
            try {
                jedis.disconnect();
            } catch (JedisException e) {
                log.error("Failed to disconnect jedis", e);
            }
        }
    }

}
