package com.inmotionsoftware.promise.Test;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.Handler;
import com.inmotionsoftware.promise.Promise.IDeferred;
import com.inmotionsoftware.tuple.Tuple;

public class Test {
	public static class MainLooper implements Executor {
		private boolean mStop = false;
		private Deque<Runnable> mQueue = new ConcurrentLinkedDeque<>();
		
		public void stop() {
			mStop = true;
		}
		
		public void run() {
			while (!mStop) {
				
				Runnable r = mQueue.pollLast();				
				if (r == null) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {}
				} else {
					r.run();
				}
			}
		}

		@Override
		public void execute(Runnable command) {
			mQueue.add(command);
		}
	}
	
	
	public static void main(String[] args) {
		System.out.println("init");
		MainLooper looper = new MainLooper();
		Promise.setMainExecutor(looper);
		
		Promise.resolve(5).then( (Integer i) -> {
			String s = Integer.toString(i);
			System.out.println("Integer: " + s);
			return s;
		});

		Promise.resolve(1)
		.then( (Integer i) -> Integer.toString(i) )
		.then( (String s) -> s + "" )
		.then( (String s) -> Tuple.make(1,2,s) )
		.then( (Integer a, Integer b, String c) -> a + "." + b + "." + c )
		.thenAsync( (String s) -> s.split("\\.") )
		.thenOnMain( (String[] s) -> {
			Integer i = Integer.parseInt(s[0]);
			System.out.println("Done!!!");
			return i;
		})
		.fail( (Throwable t) -> {
			System.out.println("Error: " + t);
		});
		
		Promise.resolve().then( (Void v) -> 5 )
		.then( (Integer i) -> {
//			if (i > 0) throw new RuntimeException("ouch!");
			return i+3;
		})
		.then( (Integer i) -> Tuple.make(i,2) )
		.then( (Integer a, Integer b) -> Integer.toString(a*b) )
		.then( (String s) -> {
			return Promise.resolve(s)
					.thenAsync( (String v) -> {
				Thread.sleep(2000);
				return ":) " + v;
			});
		})
		.then( (String s) ->  s + " :(" )
		.then(new Handler<Void,String>() {
			@Override
			public Void resolve(String in) throws Exception {
				System.out.println("finished!");
				return null;
			}
			
			@Override
			public void reject(Throwable t) {
				System.err.print("Error: " + t);
			}
			
			@Override
			public void always() {
				looper.stop();
			}
		});
		
		String strnum = "...";
		Promise.make((IDeferred<Integer> d) -> {
			try {
				Integer i = Integer.parseInt(strnum);
				d.resolve(i);
			} catch (NumberFormatException e) {
				d.reject(e);
			}
		});

		System.out.println("promise created!");
		looper.run();		
		System.out.println("Exiting...");
	}
}
