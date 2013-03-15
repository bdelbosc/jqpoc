# POC Java Job Queue using Redis

This is a proof of concept to evaluate reliable job queue using Redis.

The "reliability" is done using Redis Lua script to create atomc
primitive.

In this POC there are only producer and consumer and they only push
IDs, it's up to the consumer to agree with the producer about what
this IDs really mean.

A job that is not completed stay for ever in the list.


### Creating a queue

A queue as a name and a timeout for job completion:

    JobQueue queue = new JobQueue(queueName, timeout);


Getting the number of pending and running job:

    long pending = queue.getPendingJobCount();


Getting the number of running jobs:

	long running = queue.getRunningJobCount();

Get the total number of jobs completed including those in failure:

	long completed = queue.getCompletedJobCount():

Get the number of jobs in failure:

	long error = queue.getJobInErrorCount();

TODO: Get the list of errors

TODO: being able to configure redis access


### Producer

Producer just put job id:

	queue.addJobIds("myJobId1");
    pendingCount = queue.addJobIds("myJobId2", "myJobId3");

### Consumer

It asks for job and get a job reference:

	JobRef job = queue.getJob();


The job reference contains the jobid, a redis key and also a state:

- READY: ready to be processed
- PROCESSING: the job is processing by another worker
- TIMEDOUT: the job timedout in processing state
- NONE: no job in the queue


The consumer has to impl the following pattern:

- If a job is in a READY state, the consumer process it.
  On successful completion it calls:

        queue.jobDone(job.getKey());

  On failure it calls:

        queue.jobFailure(job.getKey(), "Some error message");

- If a job is in a PROCESSING state, the worker can ask for another
  job. After N job in PROCESSING state it can have a rest doing a
  small pause.

- If a job is in a TIMEDOUT state, the worker can check in an
  application-specific way the state of the job, and decide to
  cancel or resumbit the job in an atomic way:

       TODO: JobRef newJob = queue.resumeJob(job.getKey());

- If there are no job (NONE state) then the worker can have small
  rest.


## Requierment

- Redis 2.6 to get LUA support:

        wget http://redis.googlecode.com/files/redis-2.6.11.tar.gz
        tar xzf redis-2.6.11.tar.gz
        cd redis-2.6.11
        make


- Jedis 2.2.0-SNAPSHOT to get eval fixes

        git clone https://github.com/xetorthio/jedis.git
        cd jedis
        mvn install


## Run the poc

From eclipse edit Producer.java to choose number of thread and jobs to create
and Run as Java application.

Same for Consumer.java edit and run.

Note that there are Metrics configured to report pending jobs queue on a
Graphite server.


## References

- [Redis relieable queues with Lua script](http://oldblog.antirez.com/post/250)


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>
