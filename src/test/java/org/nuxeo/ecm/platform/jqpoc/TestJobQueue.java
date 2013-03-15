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

    private static final String HOST = "localhost";

    private static final int PORT = 6379;

    private static final String QNAME = "test";

    private static final int QTIMEOUT = 1;

    private JobQueue q;

    @Override
    protected void setUp() throws Exception {
        q = new JobQueue(HOST, PORT, QNAME, QTIMEOUT);
        q.drop();
    }

    @Override
    protected void tearDown() throws Exception {
        if (q != null) {
            q.disconnect();
        }
    }

    public void testAddJob() throws InterruptedException {
        long count;
        assertEquals(0, q.getPendingJobCount());
        count = q.addJobIds("j1");
        assertEquals(q.getPendingJobCount(), count);
        count = q.addJobIds("j2", "j3", "j4");
        assertEquals(q.getPendingJobCount(), count);
        assertEquals(4, count);
    }

    public void testEmptyQueue() throws InterruptedException {
        assertEquals(0, q.getFailureJobCount());
        assertEquals(0, q.getCompletedJobCount());
        assertEquals(0, q.getPendingJobCount());
        assertEquals(0, q.getRunningJobCount());
        assertEquals(0, q.getResumeJobCount());
        assertEquals(null, q.getLastFailure());
        assertEquals(null, q.getFailure(10));

        JobRef j = q.getJob();
        assertEquals(JobState.NONE, j.getState());
        j = q.jobResume("non-existing-id");
        assertEquals(JobState.NONE, j.getState());
        long ret = q.jobDone("non-existing-id");
        assertEquals(0, ret);
        ret = q.jobFailure("non-existing-id", "Error");
        assertEquals(0, ret);

        assertEquals(0, q.getFailureJobCount());
        assertEquals(0, q.getCompletedJobCount());
        assertEquals(0, q.getPendingJobCount());
        assertEquals(0, q.getRunningJobCount());
        assertEquals(0, q.getResumeJobCount());
    }

    public void testJobState() throws InterruptedException {
        JobRef j = q.getJob();
        assertEquals(JobState.NONE, j.getState());
        q.addJobIds("j1");
        j = q.getJob();
        assertEquals(JobState.READY, j.getState());
        j = q.getJob();
        assertEquals(JobState.PROCESSING, j.getState());
        q.jobDone(j.getKey());
        j = q.getJob();
        assertEquals(JobState.NONE, j.getState());
    }

    public void testJobCompletion() throws InterruptedException {
        q.addJobIds("j0");
        JobRef j = q.getJob();
        long ret = q.jobDone(j.getKey());
        assertEquals(1, ret);
        ret = q.jobDone(j.getKey());
        assertEquals(0, ret);

        q.addJobIds("j1");
        j = q.getJob();
        assertEquals(1, q.getRunningJobCount());
        String msg = "Error message";
        ret = q.jobFailure(j.getKey(), msg);
        assertEquals(1, ret);
        ret = q.jobFailure(j.getKey(), "Error message bis not allowed");
        assertEquals(0, ret);
        assertEquals(0, q.getRunningJobCount());
        ret = q.jobDone(j.getKey()); // not taken in account
        assertEquals(0, ret);
        assertEquals(1, q.getFailureJobCount());
        assertEquals(2, q.getCompletedJobCount());

        JobFailure f = q.getLastFailure();
        assertNotNull(f);
        assertEquals(msg, f.getMessage());
        assertEquals(f.getId(), "j1");
        log.info(f);
    }

    public void testJobResume() throws InterruptedException {
        long count;
        assertEquals(0, q.getResumeJobCount());
        assertEquals(0, q.getRunningJobCount());
        count = q.addJobIds("j1");
        assertEquals(1, count);

        JobRef j = q.getJob();
        assertEquals(JobState.READY, j.getState());
        assertEquals(1, q.getPendingJobCount());
        assertEquals(1, q.getRunningJobCount());

        Thread.sleep(QTIMEOUT * 1010L);
        j = q.getJob();
        assertEquals(JobState.TIMEDOUT, j.getState());

        j = q.jobResume(j.getKey());
        log.info(j);
        assertEquals(JobState.READY, j.getState());
        assertEquals(1, q.getResumeJobCount());
        assertEquals(1, q.getRunningJobCount());

        q.jobDone(j.getKey());
        assertEquals(1, q.getResumeJobCount());
        assertEquals(0, q.getRunningJobCount());
        assertEquals(1, q.getCompletedJobCount());
    }

    public void testJobMisc() throws InterruptedException {
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

        count = q.jobFailure(ref.getKey(), "Error on job");
        assertEquals(1, count);
        assertEquals(1, q.getFailureJobCount());

        count = q.jobDone(ref.getKey());
        assertEquals(0, count); // already removed

        Thread.sleep(QTIMEOUT * 1010L);

        for (int i = 0; i < 2; i++) {
            ref = q.getJob();
            log.info(ref);
            assertEquals(JobState.TIMEDOUT, ref.getState());
            count = q.jobDone(ref.getKey());
            assertEquals(1, count);
        }

        ref = q.getJob();
        assertEquals(JobState.NONE, ref.getState());

        q.addJobIds("j4");
        assertEquals(3, q.getCompletedJobCount());
        assertEquals(1, q.getPendingJobCount());
        assertEquals(0, q.getRunningJobCount());
        count = q.drop();
        assertEquals(0, q.getCompletedJobCount());
        assertEquals(0, q.getPendingJobCount());
        assertEquals(0, q.getRunningJobCount());

        assertEquals(1, count);
        count = q.drop();
        assertEquals(0, count);

    }

}
