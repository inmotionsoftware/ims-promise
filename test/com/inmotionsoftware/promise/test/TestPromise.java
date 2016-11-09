package com.inmotionsoftware.promise.test;

import static org.junit.Assert.assertNotEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.IDeferred;

public class TestPromise extends PromiseTestCase {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD) // on class level
	public @interface AsyncTest {
		String group() default "";
	}

	private Thread mMain;

	@Override
	protected void setUp() throws Exception {
		mMain = Thread.currentThread();
		super.setUp();
	}

	public void assertIsMainThread() {
		Thread t = Thread.currentThread();
		assertEquals(mMain, t);
	}

	public void assertIsBackgroundThread() {
		assertNotEquals(mMain, Thread.currentThread());
	}

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
	public Promise<Void> testFailDefaultsToMain() {
		Exception e = new RuntimeException();
		return Promise.reject(e).fail((Throwable t) -> {
			assertIsMainThread();
		});
	}
	
	@AsyncTest(group="testFail")
	public Promise<Void> testFailOnMain() {
		Exception e = new RuntimeException();
		return Promise.reject(e).failOnMain((Throwable t) -> {
			assertIsMainThread();
		});
	}

	@AsyncTest(group="testFail")
	public Promise<Void> testFailAsync() {
		Exception e = new RuntimeException();
		return Promise.reject(e).failAsync((Throwable t) -> {
			assertIsBackgroundThread();
		});
	}
	
	@AsyncTest(group="testFail")
	public Promise<Void> testFailIsCalled() {

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
			deferred.resolve(1);
//			throw new RuntimeException();
//			assertIsMainThread();
//			assertIsBackgroundThread();

		});
	}
}
