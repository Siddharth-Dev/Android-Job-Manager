package com.sj.android.job;


public interface Task<T> {

	public T executeTask() throws Exception;
}
