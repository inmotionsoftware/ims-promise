package com.inmotionsoftware.promise.Test;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadPoolExecutor;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.Deferrable;
import com.inmotionsoftware.promise.Promise.Handler;
import com.inmotionsoftware.promise.Promise.IDeferred;
import com.inmotionsoftware.promise.util.MainLooper;
import com.inmotionsoftware.tuple.Tuple;

public class Test {
	public class MyDeferred implements Deferrable<String> {

		@Override
		public void run(IDeferred<String> resolve) {

		}		
	}
	
	public static void testAsync() {
		final Thread main = Thread.currentThread(); 
		
		Promise.makeAsync((IDeferred<String> deferred) -> {
			assert(!Thread.currentThread().equals(main));
		});

		Promise.resolve(123).thenAsync( (Integer i) -> {
			assert(!Thread.currentThread().equals(main));
			return null;
		});		
	}
	
	public static void testReject() {

		Promise.reject(new RuntimeException("blah"))
		.then( new Handler<Void,Object>() {
			private boolean suc = false;
			
			public Void resolve(Object in) throws Exception {
				suc = true;
				assert(false);
				return null; 
			}
			
			public void always() {
				assert(!suc);
			}
			
			@Override
			public void reject(Throwable t) {
				suc = false;
			}
		} );
	}
	
	
	public static void main(String[] args) {
		
		testAsync();
		testReject();
		
//		long keepAlive = 10;
//		int min = 1;
//		int max = 4;
//		ThreadPoolExecutor exec = new ThreadPoolExecutor(min, max, keepAlive, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
//		Promise<String> p =  Promise.make(new MyDeferred(), exec);
//		
//		
//		Promise.make( (IDeferred<String> deferred) -> {
//			new Thread(() -> {
//				try {
//					String res = doSomething();
//					deferred.resolve(res);
//				} catch (Exception e) {
//					deferred.reject(e);
//				}
//			}).start();
//		});
		

		
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
