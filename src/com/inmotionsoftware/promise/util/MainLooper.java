package com.inmotionsoftware.promise.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainLooper implements Executor {
	private boolean mStop = false;
	private final BlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();
	
	public void stop() {
		mStop = true;
	}
	
	public void run() {
		while (!mStop) {				
			try {
				Runnable r = mQueue.take();
				assert(r != null);
				r.run();
			} catch (InterruptedException e) {}
		}
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
			} catch (InterruptedException e) {}			
		}
	}

	@Override
	public void execute(Runnable command) {
		assert(command != null);
		mQueue.add(command);
	}
}