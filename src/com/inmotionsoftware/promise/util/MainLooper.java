package com.inmotionsoftware.promise.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainLooper implements Executor {
	private volatile boolean mStop = false;
	private final BlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();	
	Thread mThread = null;
	
	public void stop() {
		mStop = true;

	}
	
	public void run() {
		while (!mStop) poll(100, TimeUnit.MILLISECONDS);
	}
	
	public void poll() {

		// exhaust all items on the queue, then return
		while (!mStop) {
			Runnable r = mQueue.poll();
			if (r == null) break;
			r.run();
		}
	}
	
	public void poll(long timeout, TimeUnit unit) {

		// exhaust all items on the queue, then return
		while (!mStop) {
			try {
				
				// wait a brief amount of time before bailing
				Runnable r = mQueue.poll(timeout, unit);
				if (r == null) break;
				
				r.run();
			} catch (InterruptedException e) {
				break;
			}			
		}
	}

	@Override
	public void execute(Runnable command) {
		assert(command != null);
		mQueue.add(command);
	}
}