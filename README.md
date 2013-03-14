# Job Queue POC using Redis


This is a POC to test reliable job queue using Redis.

It uses Lua script to ensure atomic primitive.

In this poc there are only producer and consumer and they only push
IDs, it's up to the consumer to agree with the producer about what
this IDs really mean.


### Creating a queue

A queue as a name and a timeout for job completion:

    JobQueue queue = new JobQueue(queueName, timeout);


Getting the size of pending job:

    long pending = queue.getSize();
	

Getting the size of running jobs:

	long running = queue.getRunningJob();


TODO: Get completed and errors jobs

TODO: being able to configure redis access


### Producer

Producer just put job id: 

	queue.addJobId("myJobId1");

### Consumer

It asks for job and get a job reference:

	JobRef job = queue.getJob();


The job reference contains the jobid, a redis key and also a state:

- READY: ready to be processed
- PROCESSING: already processed by another worker
- TIMEDOUT: the job timedout in processing state
- NONE: no job in the queue


The consumer has to impl the following pattern:

- If a job is in a READY state, the consumer process it.
  On successful completion it calls:
  
        queue.completedJob(job.getKey());

  On failure TODO
     
  
- If a job is in a PROCESSING state, it can ask for another job.
  If it get N jobs in processing state it can sleep a bit.

- If a job is in a TIMEDOUT state, the worker can check in an
  application-specific way the state of the job, and decide to 
  cancel or resumbit the job in an atomic way:

        JobRef newJob = queue.resumeJob(job.getKey());


- If there are no job (NONE state) then the worker can sleep.


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

Note that there are Metrics Graphite reporter to trace the size of the
pending jobs.


## References

- [Redis relieable queues with Lua script](http://oldblog.antirez.com/post/250)


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>
