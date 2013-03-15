package org.nuxeo.ecm.platform.jqpoc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;

/**
 * Job Queue
 *
 * Not thread safe because it uses a non thread safe jedis connection.
 */
public class JobQueue {

    private static final Log log = LogFactory.getLog(JobQueue.class);

    private final Jedis jedis;

    private final String name;

    private final long timeout;

    private static final String GETJOB_SCRIPT = "local v = redis.call('RPOP',ARGV[1]) " //
            + "if not v then return v end " //
            + "local nv = v " //
            + "if not string.find(v,'*') then" //
            + " v = v..'*'..ARGV[2] redis.call('LPUSH','run:'..ARGV[1],v) end " //
            + "redis.call('LPUSH',ARGV[1],v)" //
            + "return nv";

    private static final String RESUME_SCRIPT = "local r = redis.call('LREM',ARGV[1],1,ARGV[2]) " //
            + "if r == 0 then return nil end "
            + "redis.call('LREM','run:'..ARGV[1],1,ARGV[2]) " //
            + "redis.call('INCR','resume:'..ARGV[1]) " //
            + "local v = string.sub(ARGV[2],1,string.find(ARGV[2],'*')-1)..'*'..ARGV[3] " //
            + "redis.call('LPUSH','run:'..ARGV[1],v) " //
            + "redis.call('LPUSH',ARGV[1],v)" //
            + "return v";

    private static final String COMPLETED_SCRIPT = "local r = redis.call('LREM',ARGV[1],1,ARGV[2]) " //
            + "if r == 0 then return r end "
            + "redis.call('LREM','run:'..ARGV[1],1,ARGV[2]) " //
            + "redis.call('INCR','done:'..ARGV[1]) " //
            + "if ARGV[3] then" //
            + " redis.call('INCR','error:'..ARGV[1])" //
            + " redis.call('LPUSH','errlst:'..ARGV[1],ARGV[2]..':'..ARGV[3])" //
            + " redis.call('LTRIM','errlst:'..ARGV[1],0,99) " //
            + "end return r";

    private final String getJobScriptSha;

    private final String completedScriptSha;

    private final String resumeScriptSha;

    private final String host;

    private final int port;

    /**
     * Creating a JobQueue
     *
     * @param host the redis host
     * @param port the redis port default is 6379
     * @param name the name of the queue
     * @param timeout the maximum time a job can stay in processing state
     */
    public JobQueue(final String host, final int port, final String name,
            final long timeout) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.timeout = timeout;
        if (log.isDebugEnabled()) {
            log.debug("Creating JobQueue: " + name + ", timeout: " + timeout
                    + "s using redis " + host + ":" + port);
        }
        jedis = new Jedis(host, port);
        getJobScriptSha = jedis.scriptLoad(GETJOB_SCRIPT);
        resumeScriptSha = jedis.scriptLoad(RESUME_SCRIPT);
        completedScriptSha = jedis.scriptLoad(COMPLETED_SCRIPT);

        Metrics.defaultRegistry().newGauge(JobQueue.class, "pending-" + name,
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return getPendingJobCount();
                    }
                });

        Metrics.defaultRegistry().newGauge(JobQueue.class, "running-" + name,
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return getRunningJobCount();
                    }
                });

        Metrics.defaultRegistry().newGauge(JobQueue.class, "completed-" + name,
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return getCompletedJobCount();
                    }
                });

        Metrics.defaultRegistry().newGauge(JobQueue.class, "error-" + name,
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return getFailureJobCount();
                    }
                });

    }

    /**
     * Add a list of job IDs to the queue.
     *
     * It's up to the consumer to agree with the producer about what this IDs
     * really mean
     *
     * @param jids
     * @return
     */
    public long addJobIds(String... jids) {
        return jedis.lpush(name, jids);
    }

    /**
     * Get a job from the queue.
     *
     * The JobRef contains a state, a job id and key
     *
     * @return Always return a JobRef
     */
    public JobRef getJob() {
        final long now = (long) (System.currentTimeMillis() / 1000);
        final String timestamp = String.format("%d", now);
        String key = (String) jedis.evalsha(getJobScriptSha, 0, name, timestamp);
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

    /**
     * Mark the job as successfully completed
     *
     * @param key The key of the job
     * @return 0 on failure
     */
    public long jobDone(final String key) {
        return (Long) jedis.evalsha(completedScriptSha, 0, name, key);
    }

    /**
     * Mark the job as completed in failure.
     *
     * Note that only N last errors are kept.
     *
     * @param key The key of the job
     * @param message An error message
     * @return 0 on failure
     */
    public long jobFailure(final String key, final String message) {
        return (Long) jedis.evalsha(completedScriptSha, 0, name, key, message);
    }

    public JobRef jobResume(final String key) {
        final long now = (long) (System.currentTimeMillis() / 1000);
        final String timestamp = String.format("%d", now);
        final String newKey = (String) jedis.evalsha(resumeScriptSha, 0, name,
                key, timestamp);
        if (newKey == null) {
            // The job has been resumed by another worker
            return new JobRef(null, null, null, JobState.NONE);
        }
        return new JobRef(newKey, newKey.split("\\*")[0], timestamp,
                JobState.READY);

    }

    /**
     * Remove all the data concerning the queue.
     *
     * @return 0 on failure
     */
    public long drop() {
        jedis.del("errlst:" + name);
        jedis.del("error:" + name);
        jedis.del("done:" + name);
        jedis.del("run:" + name);
        jedis.del("resume:" + name);
        return jedis.del(name);
    }

    /**
     * Get the number of pending jobid in the queue. This include job that are
     * in the processing state.
     *
     */
    public long getPendingJobCount() {
        return jedis.llen(name);
    }

    /**
     * Get the number of completed job that was reported in failure.
     *
     * @return
     */
    public long getFailureJobCount() {
        try {
            return Long.valueOf(jedis.get("error:" + name));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the total number of completed job including those in failure.
     *
     * @return
     */
    public long getCompletedJobCount() {
        try {
            return Long.valueOf(jedis.get("done:" + name));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the number of Job that are in processing state.
     *
     */
    public long getRunningJobCount() {
        try {
            return jedis.llen("run:" + name);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the number of Job that has been timed out and resume.
     *
     */
    public long getResumeJobCount() {
        try {
            return Long.valueOf(jedis.get("resume:" + name));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Disconnect from the queue.
     *
     */
    public void disconnect() {
        if (log.isDebugEnabled()) {
            log.debug("Disconnecting JobQueue: " + name + " form redis " + host
                    + ":" + port);
        }
        if (jedis != null) {
            try {
                jedis.disconnect();
            } catch (JedisException e) {
                log.error("Failed to disconnect jedis", e);
            }
        }
    }

}
