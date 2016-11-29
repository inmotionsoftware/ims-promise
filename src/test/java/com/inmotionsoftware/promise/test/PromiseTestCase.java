package com.inmotionsoftware.promise.test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.AggregateResults;
import com.inmotionsoftware.promise.util.MainLooper;

import junit.framework.TestCase;;import org.junit.Assert;
import org.junit.Before;

 class PromiseTestCase {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD) // on class level
	@interface AsyncTest {
		String group() default "";
	}

	private Thread mMain;
	private Thread mBG;
	private ThreadPoolExecutor mPool;

	void assertIsMainThread() {
		Assert.assertEquals(mMain, Thread.currentThread());
	}

	void assertIsBackgroundThread() {
		Assert.assertEquals(mBG, Thread.currentThread());
	}
	
	
	private HashMap<String, Collection<Method>> mPromiseGroup = null;

	@Before
	protected void setUp() throws Exception {

		mMain = Thread.currentThread();
		mPool = new ThreadPoolExecutor(1, 1, 99, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {			
			@Override
			public Thread newThread(Runnable r) {
				mBG = new Thread(r);
				return mBG;
			}
		});
		
		mPromiseGroup = new HashMap<>();
		
		Class<TestPromise> clazz = TestPromise.class;
		Method[] methods = clazz.getMethods();
		
		for (Method method : methods) {
			Annotation ann = method.getAnnotation(AsyncTest.class);
			if (ann == null) continue;

			AsyncTest async = (AsyncTest)ann;
			Collection<Method> col = mPromiseGroup.get(async.group());
			if (col == null) {
				col = new ArrayList<>();
				mPromiseGroup.put(async.group(), col);
			}
			col.add(method);
		}		
	}
	
	
	void runTests(String group) {
		
		final MainLooper loop = new MainLooper();
		Promise.setMainExecutor(loop);
		Promise.setBackgroundExecutor(mPool);
		
		Collection<Promise<Object>> promises = new ArrayList<>();
		Collection<Method> methods = mPromiseGroup.get(group);
		for (Method method : methods) {
			try {
				Object obj = method.invoke(this);
				@SuppressWarnings("unchecked")
				Promise<Object> p = (Promise<Object>)obj;
				promises.add(p);
			} catch (Throwable e) {}
		}

		if (promises.size() == 0) return;
		
		@SuppressWarnings("unchecked")
		final AggregateResults<Object>[] results = new AggregateResults[1];

		Promise.all(promises)
		.then(new Promise.Handler<Void,Promise.AggregateResults<Object>>() {

			@Override
			public Void resolve(AggregateResults<Object> res) throws Exception {
				results[0] = res;
				return null;
			}
		
			@Override
			public void always() {
				loop.stop();					
			}
			
			@Override
			public void reject(Throwable t) {
				Assert.fail();
			}
		});

		loop.run();
		
		List<Throwable> fails = results[0].getFailures();
		List<Object> sucs = results[0].getSuccesses();
		
		Assert.assertTrue(fails.size() == 0);
		Assert.assertEquals(promises.size(), sucs.size());
	}
}
