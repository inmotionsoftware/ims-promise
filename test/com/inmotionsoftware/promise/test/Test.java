package com.inmotionsoftware.promise.test;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.Handler;
import com.inmotionsoftware.promise.Promise.IDeferred;
import com.inmotionsoftware.promise.util.MainLooper;
import com.inmotionsoftware.tuple.Tuple;

public class Test {
	private static Thread mMain;
	
	public static boolean isMainThread() {
		return Thread.currentThread().equals(mMain);
	}
	
	public static boolean isBackgroundThread() {
		return !isMainThread();
	}
	
	public static void printCurrentThread() {
		System.out.println("Thread is " + getThreadName());
	}
	
	public static String getThreadName() {
		return (isMainThread() ? "Main" : "Background");
	}
	
	public static void testAlways() {
		final boolean[] suc = new boolean[] { false };
		Promise.resolve().always(() -> {
			suc[0] = true;
		});
		assert(suc[0]);
		
		Promise.resolve().thenAsync( () -> {} ).always( () -> {	
			assert(isBackgroundThread());
		});
		
		Promise.resolve().thenOnMain( () -> {} ).always( () -> {	
			assert(isMainThread());
		});
		
		Promise.resolve().thenAsync( () -> {} ).alwaysOnMain( () -> {	
			assert(isMainThread());
		});
		
		Promise.resolve().alwaysOnMain( () -> {	
			assert(isMainThread());
		});
		
		Promise.resolve().alwaysAsync( () -> {	
			assert(isBackgroundThread());
		});
	}
	
	public static void testFail() {
		Exception e = new RuntimeException();
		Promise.reject(e).fail( (Throwable t) -> {
			assert(isMainThread());
		});
		
		Promise.reject(e).failOnMain( (Throwable t) -> {
			assert(isMainThread());
		});
		
		Promise.reject(e).failAsync( (Throwable t) -> {
			assert(isBackgroundThread());
		});
		
		Promise.make( (IDeferred<Integer> def) -> {
			throw new RuntimeException();
		})
		.fail((Throwable t) -> {
			assert(isMainThread());
			assert(t != null);
		});
	}
	
	public static void testAsync() {
		assert(isMainThread());
		
		Promise.makeAsync((IDeferred<String> deferred) -> {
			printCurrentThread();
			assert(isBackgroundThread());
			deferred.resolve("done");
		});
		
		Promise.make((IDeferred<Integer> deferred) -> {
			printCurrentThread();
			assert(isMainThread());
			deferred.resolve(1);
		});
		
		Promise.makeOnMain((IDeferred<Integer> deferred) -> {
			printCurrentThread();
			assert(isMainThread());
			deferred.resolve(1);
		});

		Promise.resolve(123)
		.thenAsync( (Integer i) -> {
			printCurrentThread();
			assert(isBackgroundThread());
			return ++i;
		})
		.thenOnMain( (Integer i) -> {
			printCurrentThread();
			assert(isMainThread());
			return ++i;
		});
		
		Promise.makeAsync((IDeferred<Integer> def) -> {
			printCurrentThread();
			assert(isBackgroundThread());
			def.resolve(5);
		})
		.then( (Integer i) -> {
			printCurrentThread();
			assert(isBackgroundThread());
			return ++i;
		})
		.thenOnMain( (Integer i) -> {
			printCurrentThread();
			assert(isMainThread());
			return ++i;
		})
		.thenAsync( (Integer i) -> {
			printCurrentThread();
			assert(isBackgroundThread());
			throw new RuntimeException("back to main");
		})
		.always( () -> {
			System.out.println("always called");
			printCurrentThread();
			assert(isBackgroundThread());			
		})
		.failOnMain( (Throwable t) -> {
			System.out.println("failOnMain");
			printCurrentThread();
			assert(isMainThread());
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
		});
		
		Promise.make( (IDeferred<String> res) -> {
			throw new RuntimeException("Error");			
		}).then(new Handler<Void,String>() {
			
			boolean suc = false;
			
			@Override
			public Void resolve(String in) throws Exception {
				suc = true;
				return null; 
			}
			
			@Override
			public void always() {
				assert(!suc);
			}
			
			@Override
			public void reject(Throwable t) {
				suc = false;
			}
		}).then( (Void v) -> {
			assert(false);
			return null;
		});
	}
	
	public static void main(String[] args) {
		mMain = Thread.currentThread();
		
		testFail();
		testAlways();
		testAsync();
		testReject();
		
		System.out.println("init");
		MainLooper looper = new MainLooper();
		Promise.setMainExecutor(looper);
		
		// Make sure that the various void style lambdas will compile
		Promise.resolve().then( () -> "" ).then( (String s) -> {} ).then( () -> {} );
		
		Promise.resolve(1).then( () -> {} );

//		Promise.resolve(1).then( new Handler<Void,Void>() {
//			@Override
//			public Void resolve(Void in) throws Exception {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		});
		
		Promise.resolve(1).then( (Integer i) -> { } );
		
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
