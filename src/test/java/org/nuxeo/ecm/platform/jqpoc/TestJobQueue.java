package org.nuxeo.ecm.platform.jqpoc;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.jqpoc.JobQueue;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Job Queue Test
 *
 */
public class TestJobQueue extends TestCase {
    private static final Log log = LogFactory.getLog(TestJobQueue.class);

    public void testJobQueue() {
        JobQueue q = new JobQueue("foo");
        try {
            q.flush();
            long count;
            count = q.addJob("j1");
            assertEquals(1, count);
            count = q.addJob("j2");
            count = q.addJob("j3");
            assertEquals(3, count);

            String ret;
            String j1 = q.getJob();
            assertTrue(j1.contains("j1"));
            log.info("getJob " + j1);
            ret = q.getJob();
            ret = q.getJob();
            assertTrue(ret.contains("j3"));

            count = q.completedJob(j1);
            assertEquals(1, count);

            count = q.addJob("j4");
            assertEquals(3, count);

            count = q.flush();
            assertEquals(1, count);
            count = q.flush();
            assertEquals(0, count);

        } finally {
            q.close();
        }
    }

}
