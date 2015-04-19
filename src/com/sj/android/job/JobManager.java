package com.sj.android.job;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobManager {

	// Sets the amount of time an idle thread will wait for a task before
	// terminating
	private static final int KEEP_ALIVE_TIME = 1;

	// Sets the Time Unit to seconds
	private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

	// Sets the initial threadpool size to 8
	private static final int CORE_POOL_SIZE = 2;

	// Sets the maximum threadpool size to 8
	private static final int MAXIMUM_POOL_SIZE = 8;

	// A queue of Runnables for the API pool
	private final BlockingQueue<Runnable> mJobApiWorkQueue;

	// A managed pool of background API threads
	private final ThreadPoolExecutor mApiThreadPool;
	// A single instance of JobManager, used to implement the singleton
	// pattern
	private static JobManager sInstance = null;

	// A Temporary Map to hold the job being in queue so that they can be
	// referred if needed
	private static HashMap<Integer, Job<?>> runningJobList = new HashMap<Integer, Job<?>>();

	// A static block that sets class fields
	static {

		// The time unit for "keep alive" is in seconds
		KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

		// Creates a single static instance of PhotoManager
		sInstance = new JobManager();
	}

	/**
	 * Constructs the work queues and thread pool
	 */
	private JobManager() {

		/*
		 * Creates a work queue for the pool of Thread objects used for
		 * decoding, using a linked list queue that blocks when the queue is
		 * empty.
		 */
		mJobApiWorkQueue = new LinkedBlockingQueue<Runnable>();

		/*
		 * Creates a new pool of Thread objects for the download work queue
		 */
		mApiThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE,
				MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
				mJobApiWorkQueue);

	}

	public static JobManager getInstance() {
		return sInstance;
	}

	public int addJob(Job<?> job) {
		// Putting job in a Map so that it can be referred later
		runningJobList.put(job.getTag(), job);
		
		// Putting Job in execution queue
		mApiThreadPool.execute(job);

		// returning job id so that UI can use it for operations like cancel
		return job.getTag();
	}

	public void jobFinished(int jobTag) {
		runningJobList.remove(jobTag);
	}

	/**
	 * Cancels all Threads in the ThreadPool
	 */
	public static void cancelAll() {

		/*
		 * Creates an array of tasks that's the same size as the task work queue
		 */
		Job<?>[] taskArray = new Job<?>[sInstance.mJobApiWorkQueue.size()];

		// Populates the array with the task objects in the queue
		sInstance.mJobApiWorkQueue.toArray(taskArray);

		// Stores the array length in order to iterate over the array
		int taskArraylen = taskArray.length;

		/*
		 * Locks on the singleton to ensure that other processes aren't mutating
		 * Threads, then iterates over the array of tasks and interrupts the
		 * task's current Thread.
		 */
		synchronized (sInstance) {

			// Iterates over the array of tasks
			for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {

				// Gets the task's current thread
				Thread thread = taskArray[taskArrayIndex].getJobsThread();

				// if the Thread exists, post an interrupt to it
				if (null != thread) {
					thread.interrupt();
				}
			}
		}
	}

	/**
	 * Stops a API Thread and removes it from the threadpool
	 * 
	 * @param taskId
	 *            Id that is returned when a job is added to the pool
	 */
	static public void cancelTask(int taskId) {

		// If the Thread object still exists and the download matches the
		// specified URL
		Job<?> job = runningJobList.get(taskId);
		if (null != job) {
			
			if(sInstance.mJobApiWorkQueue.contains(job)){
				sInstance.mJobApiWorkQueue.remove(job);
				sInstance.jobFinished(taskId);
				return;
			}

			/*
			 * Locks on this class to ensure that other processes aren't
			 * mutating Threads.
			 */
			synchronized (sInstance) {

				// Gets the Thread that the task is running on
				Thread thread = job.getJobsThread();

				// If the Thread exists, posts an interrupt to it
				if (null != thread)
					thread.interrupt();
			}
			/*
			 * Removes the Job Runnable from the ThreadPool. This opens a Thread
			 * in the ThreadPool's work queue, allowing a task in the queue to
			 * start.
			 */
			sInstance.mApiThreadPool.remove(job);
			sInstance.jobFinished(taskId);
		}
	}

	public static void cancelAllTask() {

		if (runningJobList.size() > 0) {
			/*
			 * Locks on the singleton to ensure that other processes aren't
			 * mutating Threads, then iterates over the array of tasks and
			 * interrupts the task's current Thread.
			 */
			synchronized (sInstance) {

				Iterator<Entry<Integer, Job<?>>> iterator = runningJobList.entrySet()
						.iterator();
				// Iterates over the array of tasks
				while (iterator.hasNext()) {
					// Gets the task's current thread
					
					Job<?> job = iterator.next().getValue();
					if (null != job) {
						
						if(sInstance.mJobApiWorkQueue.contains(job)){
							sInstance.mJobApiWorkQueue.remove(job);
							iterator.remove();
							//sInstance.jobFinished(job.getTag());
							continue;
						}

						
							// Gets the Thread that the task is running on
							Thread thread = job.getJobsThread();

							// If the Thread exists, posts an interrupt to it
							if (null != thread)
								thread.interrupt();
												/*
						 * Removes the Job Runnable from the ThreadPool. This opens a Thread
						 * in the ThreadPool's work queue, allowing a task in the queue to
						 * start.
						 */
						sInstance.mApiThreadPool.remove(job);
						iterator.remove();
						//sInstance.jobFinished(job.getTag());
					}
				}
				
			}
			
			
		}
	}

	public static void cancelAllPendingTask() {

		synchronized (sInstance) {
			Runnable runnable = null;
			do {
				runnable = sInstance.mJobApiWorkQueue.poll();
			} while (runnable != null);
		}
	}
}
