package org.nuxeo.ecm.platform.jqpoc;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Job Queue Test
 *
 */
public class TestJobQueue extends TestCase {
    private static final Log log = LogFactory.getLog(TestJobQueue.class);

    public void testJobQueue() {
        JobQueue q = new JobQueue("foo", 1);
        try {
            q.flush();
            long count;
            count = q.addJobId("j1");
            assertEquals(1, count);
            count = q.addJobId("j2");
            count = q.addJobId("j3");
            assertEquals(3, count);

            JobRef ref;
            JobRef j1 = q.getJob();
            log.info("getJob " + j1);

            assertEquals("j1", j1.getId());
            assertEquals(JobState.READY, j1.getState());

            ref = q.getJob();
            ref = q.getJob();
            assertEquals("j3", ref.getId());

            count = q.completedJob(j1.getKey());
            assertEquals(1, count);

            count = q.addJobId("j4");
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
