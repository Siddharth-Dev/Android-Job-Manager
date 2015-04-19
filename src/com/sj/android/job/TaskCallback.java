package com.sj.android.job;


public interface TaskCallback<T> {
	void onStart();
    void onSuccess(T obj);
    void onFailure(Exception exception);
}
