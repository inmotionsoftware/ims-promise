package com.inmotionsoftware.promise.test;

import org.junit.Test;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.IDeferred;

public class TestPromise extends PromiseTestCase {

	@Test
	public void testAlways() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}
	
	@Test
	public void testFail() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}
	
	@Test
	public void testAsync() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}

	@Test
	public void testPromisePromise() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}
	
	@AsyncTest(group = "testPromisePromise")
	public Promise<Void> testNullPromise() {
		
		final Throwable[] err = new Throwable[] {null};
		return Promise.resolve("String")
		.thenAsync( (String result) -> {
			return result;
        }).thenOnMain( (String result) -> {
        	// return null promise
            return (Promise<Void>)null;
        }).fail( (Throwable t) -> {
        	err[0] = t;
        }).always( () -> {
            assertNotNull(err[0]);
        });
	}
	
	
	@AsyncTest(group = "testAlways")
	public Promise<Void> testAlwaysCalled() {
		final boolean[] suc = new boolean[] { false };
		Promise<Void> p = Promise.resolve().always(() -> {
			suc[0] = true;
		});
		assertTrue(suc[0]);
		return p;
	}

	@AsyncTest(group = "testAlways")
	public Promise<Void> testAlwaysAsync() {
		return Promise.resolve()
			.thenAsync(() -> {})
			.always(() -> {
				assertIsBackgroundThread();
		});
	}

	@AsyncTest(group = "testAlways")
	public Promise<Void> testAlwaysOnMain() {
		return Promise.resolve()
			.thenOnMain(() -> {})
			.always(() -> {
				assertIsMainThread();
		});
	}

	@AsyncTest(group = "testAlways")
	public Promise<Void> testAlwaysOnMain2() {
		return Promise.resolve()
			.thenAsync(() -> {})
			.alwaysOnMain(() -> {
				assertIsMainThread();
		});
	}

	@AsyncTest(group = "testAlways")
	public Promise<Void> testAlwaysOnMain3() {
		return Promise.resolve()
			.alwaysOnMain(() -> {
				assertIsMainThread();
		});
	}

	@AsyncTest(group = "testAlways")
	public Promise<Void> testAlwaysAsync2() {
		return Promise.resolve()
			.alwaysAsync(() -> {
				assertIsBackgroundThread();
		});
	}
	
	@AsyncTest(group="testFail")
	public Promise<Throwable> testFailDefaultsToMain() {
		Exception e = new RuntimeException();
		return Promise.reject(e).fail((Throwable t) -> {
			assertIsMainThread();
		});
	}
	
	@AsyncTest(group="testFail")
	public Promise<Throwable> testFailOnMain() {
		Exception e = new RuntimeException();
		return Promise.reject(e).failOnMain((Throwable t) -> {
			assertIsMainThread();
		});
	}

	@AsyncTest(group="testFail")
	public Promise<Throwable> testFailAsync() {
		Exception e = new RuntimeException();
		return Promise.reject(e).failAsync((Throwable t) -> {
			assertIsBackgroundThread();
		});
	}
	
	@AsyncTest(group="testFail")
	public Promise<Throwable> testFailIsCalled() {
		return Promise.make((IDeferred<Integer> def) -> {
			throw new RuntimeException();

		}).then( (Integer i) -> {
			fail(); // shouldn't get here...

		}).fail((Throwable t) -> {
			assertIsMainThread();
			assertNotNull(t);
		});
	}
	
	@AsyncTest(group="testAsync")
	public Promise<Void> testAsyncThen() {
		Promise<Void> promise = Promise.resolve().thenAsync(()->{});

		try { // give it time to complete
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return promise.then(()-> {
			assertIsBackgroundThread();
		});
	}

	
	@AsyncTest(group="testAsync")
	public Promise<String> testMakeAsync() {
		return Promise.makeAsync((IDeferred<String> deferred) -> {
			assertIsBackgroundThread();
			deferred.resolve("done");
		});
	}


	@AsyncTest(group="testAsync")
	public Promise<Integer> testMakeIsOnMain() {
		assertIsMainThread();
		return Promise.make((IDeferred<Integer> deferred) -> {
			assertIsMainThread();
			deferred.resolve(1);
		});
	}
	
	@AsyncTest(group="testAsync")
	public Promise<Integer> testMakeOnMain() {
		assertIsMainThread();
		return Promise.makeOnMain((IDeferred<Integer> deferred) -> {
			assertIsMainThread();
			deferred.resolve(1);
		});
	}
}
