# POC Java Job Queue using Redis

This is a proof of concept to evaluate reliable job queue using Redis.

The "reliability" is done using Redis Lua script to create atomic
primitive.

In this POC there are only producer and consumer and they only push
IDs, it's up to the consumer to agree with the producer about what
this IDs really mean.

A job that is not completed stay for ever in the list.

## API

### Creating a queue

A queue as a name and a timeout for job completion:

    JobQueue queue = new JobQueue("localhost", 6379, queueName, timeout);


Getting the number of pending and running job:

    long pending = queue.getPendingJobCount();


Getting the number of running jobs:

    long running = queue.getRunningJobCount();

Get the total number of jobs completed including those in failure:

    long completed = queue.getCompletedJobCount():

Get the number of jobs in failure:

    long error = queue.getJobInErrorCount();

Drop a queue removing all the persisted data:

    queue.drop();

TODO: Get the list of errors


### Producer

Producer just put job IDs:

    queue.addJobIds("myJobId1");
    pendingCount = queue.addJobIds("myJobId2", "myJobId3");

### Consumer

It asks for job and get a job reference:

    JobRef job = queue.getJob();


The job reference contains the jobid, a redis key and also a state:

- READY: ready to be processed
- PROCESSING: the job is processing by another worker
- TIMEDOUT: the job timed out in processing state
- NONE: no job in the queue


The consumer has to impl the following pattern:

- If a job is in a READY state, the consumer process it.
  On successful completion it calls:

        queue.jobDone(job.getKey());

  On failure it calls:

        queue.jobFailure(job.getKey(), "Some error message");

- If a job is in a PROCESSING state, the worker can ask for another
  job. After N jobs in PROCESSING state it can have a rest doing a
  small pause.

- If a job is in a TIMEDOUT state, the worker can check in an
  application-specific way the state of the job, and decide to
  cancel or resumbit the job in an atomic way:

       TODO: JobRef newJob = queue.resumeJob(job.getKey());

- If there are no job (NONE state) then the worker can have small
  rest.

### Thread safety

The JobQueue use a dedicated Jedis connection which is not thread safe.
This means that JobQueue is not thread safe and should not be shared.
Each worker has to create its own JobQueue.

Using a Jedis connection pool may make the JobQueue thread safe.  But
the poc is focused on the maximum throughput on producer/worker and in
this case it is better to have one connection per producer/worker so
there are no contention on the connection pool.

## Redis data

Here is the list of data stored in Redis for a queue named <QUEUE_NAME>:

QUEUE_NAME
: The name of the queue is a redis list of pending job IDs the running
  one renamed into JOBID*UNIXTIMESTAMP, this is the refered as the job
  key.

run:QUEUE_NAME
: A list of job keys that are in processing state.

done:QUEUE_NAME
: A counter of completed job (including failures).

error:QUEUE_NAME
: A counter of job in failure.

errlst:QUEUE_NAME
: A list of the last N last errors including job keys and messages.


You can view the data using redis-cli, for instance if the name of the
queue is 'foo':

    # queue size
    redis 127.0.0.1:6379> llen foo
    (integer) 3
    # list running keys
    redis 127.0.0.1:6379> lrange run:foo 0 -1
    1) "j3*1363343422"
    2) "j2*1363343422"
    redis 127.0.0.1:6379> llen run:foo
    (integer) 2
    # get error count
    redis 127.0.0.1:6379> llen error:foo
    (integer) 1
	# get error list
    lrange  errlst:foo 0 -1
    1) "j1*1363343843:Error on job"



## Requirement

- Redis 2.6 to get the Lua support:

        wget http://redis.googlecode.com/files/redis-2.6.11.tar.gz
        tar xzf redis-2.6.11.tar.gz
        cd redis-2.6.11
        make


- Jedis 2.2.0-SNAPSHOT to get evalsha fixes

        git clone https://github.com/xetorthio/jedis.git
        cd jedis
        mvn install


## Run the poc

From eclipse edit Producer.java to choose number of thread and jobs to create
and Run as Java application.

Same for Consumer.java edit and run.

Note that there are Metrics configured to report queue information on a
Graphite server.


## References

- [Redis relieable queues with Lua script](http://oldblog.antirez.com/post/250)


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>
