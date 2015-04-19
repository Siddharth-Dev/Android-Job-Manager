package com.sj.android.job;

public class Job<T> extends BaseJob<T> {

	public static final int NOT_STARTED = 0;
	public static final int IN_PROGRESS = 1;
	public static final int COMPLETED = 2;
	private int jobStatus = NOT_STARTED;
	// private JobExecuteService<?> executeService;
	private TaskCallback<T> callback;
	private Task<T> task;
	private Thread mThread;

	public Job(TaskCallback<T> callback, Task<T> task) {
		this.callback = callback;
		this.task = task;
	}

	public void setCurrentThread(Thread thread) {
		mThread = thread;
	}

	public Thread getJobsThread() {
		return mThread;
	}

	public int getTag() {
		return this.hashCode();
	}

	public int getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(int jobStatus) {
		this.jobStatus = jobStatus;
	}

	@Override
	public void onStart() {
		if (null != callback) {
			callback.onStart();
		}
	}

	@Override
	public void onCompleted(T t) {
		if (null != callback) {
			callback.onSuccess(t);
		}
	}

	@Override
	public void onError(Exception exception) {
		if (null != callback) {
			callback.onFailure(exception);
		}
	}

	@Override
	public void run() {

		callOnStart();
		// Moves the current Thread into the background
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		setCurrentThread(Thread.currentThread());
		if (Thread.interrupted())
			return;
		try {
			T t = task.executeTask();
			if (Thread.interrupted())
				return;
			JobManager.getInstance().jobFinished(getTag());
			callOnCompleted(t);
		} catch (Exception e) {
			callOnError(e);
		}

	}

}
