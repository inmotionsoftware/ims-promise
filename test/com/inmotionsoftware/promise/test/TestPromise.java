package com.inmotionsoftware.promise.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.Handler;
import com.inmotionsoftware.promise.Promise.IDeferred;
import com.inmotionsoftware.promise.util.MainLooper;

import junit.framework.TestCase;

public class TestPromise extends TestCase {
	
	private Thread mMain;
	
	@Override
	protected void setUp() throws Exception {
		mMain = Thread.currentThread();
		super.setUp();
	}
	
	public void assertIsMainThread() {
		assertEquals(mMain, Thread.currentThread());
	}
	
	public void assertIsBackgroundThread() {
		assertNotEquals(mMain, Thread.currentThread());
	}	
	
	public void printCurrentThread() {
		System.out.println("Thread is " + getThreadName());
	}
	
	public String getThreadName() {
		boolean isMain = Thread.currentThread().equals(mMain);		
		return isMain ? "Main" : "Background";
	}
	
	@Test
	public void testAlways() {
		MainLooper loop = new MainLooper();
		
		Collection<Promise<Object>> col = new ArrayList<Promise<Object>>();
		
		final boolean[] suc = new boolean[] { false };
		Promise<? extends Object> p1 = 
		Promise.resolve().always(() -> {
			suc[0] = true;
		});
		assertTrue(suc[0]);
		col.add((Promise<Object>)p1);
		
		Promise<? extends Object> p2 = 
		Promise.resolve().thenAsync( () -> {} ).always( () -> {	
			assertIsBackgroundThread();
		});
		col.add((Promise<Object>)p2);
		
		Promise<? extends Object> p3 =
		Promise.resolve().thenOnMain( () -> {} ).always( () -> {	
			assertIsMainThread();
		});
		col.add((Promise<Object>)p3);
		
		Promise.resolve().thenAsync( () -> {} ).alwaysOnMain( () -> {	
			assertIsMainThread();
		});
		
		Promise.resolve().alwaysOnMain( () -> {	
			assertIsMainThread();
		});
		
		Promise.resolve().alwaysAsync( () -> {	
			assertIsBackgroundThread();
		});
		
		Promise.all(col).always( () -> {
			loop.stop();
		});
		
		loop.run();
	}
	
	@Test
	public void testFail() {
		Exception e = new RuntimeException();
		Promise.reject(e).fail( (Throwable t) -> {
			assertIsMainThread();
		});
		
		Promise.reject(e).failOnMain( (Throwable t) -> {
			assertIsMainThread();
		});
		
		Promise.reject(e).failAsync( (Throwable t) -> {
			assertIsBackgroundThread();
		});
		
		Promise.make( (IDeferred<Integer> def) -> {
			throw new RuntimeException();
		})
		.fail((Throwable t) -> {
			assertIsMainThread();
			assertNotNull(t);
		});
	}
	
	@Test
	public void testAsync() {
		assertIsMainThread();
		
		Promise.makeAsync((IDeferred<String> deferred) -> {
			printCurrentThread();
			assertIsBackgroundThread();
			deferred.resolve("done");
		});
		
		Promise.make((IDeferred<Integer> deferred) -> {
			printCurrentThread();
			assertIsMainThread();
			deferred.resolve(1);
		});
		
		Promise.makeOnMain((IDeferred<Integer> deferred) -> {
			printCurrentThread();
			assertIsMainThread();
			deferred.resolve(1);
		});

		Promise.resolve(123)
		.thenAsync( (Integer i) -> {
			printCurrentThread();
			assertIsBackgroundThread();
			return ++i;
		})
		.thenOnMain( (Integer i) -> {
			printCurrentThread();
			assertIsMainThread();
			return ++i;
		});
		
		Promise.makeAsync((IDeferred<Integer> def) -> {
			printCurrentThread();
			assertIsBackgroundThread();
			def.resolve(5);
		})
		.then( (Integer i) -> {
			printCurrentThread();
			assertIsBackgroundThread();
			return ++i;
		})
		.thenOnMain( (Integer i) -> {
			printCurrentThread();
			assertIsMainThread();
			return ++i;
		})
		.thenAsync( (Integer i) -> {
			printCurrentThread();
			assertIsBackgroundThread();
			throw new RuntimeException("back to main");
		})
		.always( () -> {
			System.out.println("always called");
			printCurrentThread();
			assertIsBackgroundThread();			
		})
		.failOnMain( (Throwable t) -> {
			System.out.println("failOnMain");
			printCurrentThread();
			assertIsMainThread();
		});
	}
	
	@Test
	public void testReject() {

		Promise.reject(new RuntimeException("blah"))
		.then( new Handler<Void,Object>() {
			private boolean suc = false;
			
			public Void resolve(Object in) throws Exception {
				suc = true;
				fail();
				return null; 
			}
			
			public void always() {
				assertFalse(suc);
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
				assertFalse(suc);
			}
			
			@Override
			public void reject(Throwable t) {
				suc = false;
			}
		}).then( (Void v) -> {
			fail();
			return null;
		});
	}
}
