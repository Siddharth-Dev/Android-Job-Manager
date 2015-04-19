package com.sj.android.job;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

abstract public class BaseJob<T> implements Runnable {

	private final int ADDED = 1;
	private final int COMPLETED = 2;
	private final int ERROR = 3;

	public abstract void onStart();

	public abstract void onCompleted(T t);

	public abstract void onError(Exception exception);
	
	public void callOnStart(){
		handler.sendMessage(handler.obtainMessage(ADDED));
	}
	
	public void callOnCompleted(T t){
		handler.sendMessage(handler.obtainMessage(COMPLETED, t));
	}
	
	public void callOnError(Exception exception){
		handler.sendMessage(handler.obtainMessage(ERROR, exception));
	}

	private Handler handler = new Handler(Looper.getMainLooper()) {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case ADDED:
					onStart();
				break;
			case COMPLETED:
					onCompleted((T) msg.obj);
				break;
			case ERROR:
					onError((Exception)msg.obj);
				break;
			default:
				break;
			}

		}

	};

}
