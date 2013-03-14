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

    public void testJobQueue() throws InterruptedException {
        JobQueue q = new JobQueue("foo", 1);
        try {
            q.flush();

            long count;
            count = q.addJobIds("j1");
            assertEquals(1, count);
            count = q.addJobIds("j2", "j3");
            assertEquals(3, count);

            JobRef ref;
            JobRef j1 = q.getJob();
            log.info("getJob " + j1);
            assertEquals("j1", j1.getId());
            assertEquals(JobState.READY, j1.getState());

            ref = q.getJob();
            ref = q.getJob();
            assertEquals("j3", ref.getId());
            assertEquals(JobState.READY, ref.getState());

            ref = q.getJob(); // j1
            assertEquals(j1.getKey(), ref.getKey());
            assertEquals(JobState.PROCESSING, ref.getState());

            count = q.completedJob(ref.getKey());
            assertEquals(1, count);

            count = q.completedJob(ref.getKey());
            assertEquals(0, count); // already removed

            Thread.sleep(2000L);

            for (int i = 0; i < 2; i++) {
                ref = q.getJob();
                log.info(ref);
                assertEquals(JobState.TIMEDOUT, ref.getState());
                count = q.completedJob(ref.getKey());
                assertEquals(1, count);
            }

            ref = q.getJob();
            assertEquals(JobState.NONE, ref.getState());

            q.addJobIds("j4");
            count = q.flush();
            assertEquals(1, count);
            count = q.flush();
            assertEquals(0, count);

        } finally {
            q.close();
        }
    }

}
