package com.ims.promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Promise<OUT> {
	
	private interface ExceptionHandler {
		public void error(Throwable e);
	}
	
	private interface FinallyHandler {
		public void doFinally();
	}
	
	private interface InComp<IN> extends ExceptionHandler, FinallyHandler {
		public void apply(IN in);		
	}
	
	private interface OutComp<OUT> {
		public void addChild( InComp<OUT> child );		
		public void setError(ExceptionHandler handler);
		public void setFinally(FinallyHandler handler);
	}
	
	private static class Continuation<IN,OUT> implements OutComp<OUT>, InComp<IN> {
		class Result {
			public OUT out;
			public Throwable exc;
			
			public Result(OUT o, Throwable e) {
				out = o;
				exc = e;
			}
			
			public void applyTo(InComp<OUT> child) {
				if (this.exc != null) {
					child.error(this.exc);
				} else {
					child.apply(this.out);	
				}				
				child.doFinally();
			}
		}
		
		private List< InComp<OUT> > mChildren;
		private Func<IN,OUT> mFunc;
		private Result mResult;
		
		private ExceptionHandler mCatch;
		private FinallyHandler mFinally;
		
		Continuation( Func<IN,OUT> func ) {
			mFunc = func;
		}
		
		@Override
		public void setError(ExceptionHandler handler) {
			Throwable e;
			synchronized (this) {
				mCatch = handler;
				if (mResult == null) return;
				
				e = mResult.exc;
			}
			
			assert(e != null);
			handler.error(e);
		}
		
		@Override
		public void setFinally(FinallyHandler handler) {
			synchronized (this) {
				mFinally = handler;
				if (mResult == null) return;
			}
	
			handler.doFinally();
		}
		
		@Override
		public void apply(IN in) {
			Throwable exc = null;
			OUT out = null;
			try {
				out = mFunc.apply(in);
			} catch (Exception e) {
				exc = e;
			}
			Result result = new Result(out, exc);
			
			List< InComp<OUT> > children = null;			
			synchronized (this) {				
				mResult = result;
				children = mChildren;
				mChildren = null;
			}
			
			if (result.exc != null) 
				this.error(result.exc);
	
			if (children != null) {
				for (InComp<OUT> child : children) {
					result.applyTo(child);
				}
			}
			
			this.doFinally();
		}
	
		public void doFinally() {
			if (mFinally == null) return;
			try {
				mFinally.doFinally();
			} catch (Exception e) {}
		}
		
		public void error(Throwable e) {
			if (mCatch == null) return;
	
			try {
				mCatch.error(e);
			} catch (Exception e2) {}
		}
	
		@Override
		public void addChild(InComp<OUT> child) {
			
			Result result;
			synchronized (this) {
				if (mResult == null) {
					if (mChildren == null) mChildren = new ArrayList<>();
					mChildren.add(child);
					return;
				}
	
				result = mResult;
			}		
	
			assert(result != null);			
			result.applyTo(child);
		}		
	}


	private OutComp<OUT> mOut;
	private static ThreadPoolExecutor mPool;
	private static BlockingQueue<Runnable> mQueue;
	
	static {
		int cores = Runtime.getRuntime().availableProcessors();
		mQueue = new LinkedBlockingQueue<>();
		mPool = new ThreadPoolExecutor(2, cores, 10, TimeUnit.MINUTES, mQueue);
	}

	private Promise(OutComp<OUT> out) {
		mOut = out;
	}
	
	public static<T> Promise<List<T>> all(final Iterable<Promise<T>> iter) {
		
		class PromiseList implements Callback<List<T>>, Func<T,Void>, ExceptionHandler {
			private List<T> list = new ArrayList<>();
			private AtomicInteger counter = new AtomicInteger(0);
			private Resolver<List<T>> mResolve;
			
			public Void apply( T t ) {
				synchronized (this) {
					list.add(t);	
				}				
				decrement();
				return null;
			}
			
			public void run(Resolver<List<T>> resolve) {
				mResolve = resolve;
				increment();
				for (Promise<T> promise : iter) {
					increment();
					promise.then(this);
				}
				decrement();
			}
			
			public void increment() {
				counter.incrementAndGet();
			}
			
			public void decrement() {
				if (counter.decrementAndGet() == 0) {
					mResolve.resolve(list);
				}
			}

			@Override
			public void error(Throwable e) {
				decrement();
			}
		};
		
		return Promise.make(new PromiseList());
	}
	
	public static <IN,OUT> Promise<OUT> async(final IN in, Func<IN,OUT> func) {
		final Continuation<IN, OUT> cont = new Continuation<IN, OUT>(func);

		mPool.execute(new Runnable() {				
			@Override
			public void run() {
				cont.apply(in);
			}
		});
		return new Promise<>(cont);
	}

	public static <OUT> Promise<OUT> async(Func<Void,OUT> func) {
		return async(null, func);
	}
	
	public static <OUT> Promise<OUT> sync(Func<Void,OUT> func) {
		return sync(null, func);
	}
	
	public static <IN,OUT> Promise<OUT> sync(IN in, Func<IN,OUT> func) {
		Continuation<IN, OUT> cont = new Continuation<>(func);
		cont.apply(in);			
		return new Promise<>(cont);
	}
	

	public interface Resolver<IN> {	
		public void resolve(IN in);
		public void reject(Throwable e);
	}
	
	public interface Callback<IN> {
		void run(Resolver<IN> resolve);
	}
	
	public abstract class blah<IN> implements Func<IN,OUT> {
		public OUT apply(IN in) { return resolve(in); }
		
		public abstract OUT resolve(IN in);
		public Void reject(Throwable t) { return null; }
//		public Void dofinally(Void);
	}
	
	public interface PromiseFunc<IN,OUT> extends Func<IN, Promise<OUT>> {}

	public static <OUT> Promise<OUT> resolve( OUT in ) {
		final Continuation<OUT, OUT> cont = new Continuation<>(new Func<OUT, OUT>() {
			public OUT apply(OUT in) {
				return in;
			}
		});
		cont.apply(in);
		return new Promise<>(cont);
	}
	
	public static <OUT> Promise<OUT> make( Callback<OUT> func ) {
		final Continuation<OUT, OUT> cont = new Continuation<>(new Func<OUT, OUT>() {
			public OUT apply(OUT in) {
				return in;
			}
		});

		Resolver<OUT> b = new Resolver<OUT>() {			
			public void resolve(OUT in) {
				cont.apply(in);
			}

			public void reject(Throwable e) {
				cont.error(e);
			}
		};
		func.run(b);
		return new Promise<>(cont);
	}
	
	public <RT> Promise<RT> then( final PromiseFunc<OUT,RT> func ) {
		class Proxy implements Callback<RT>, Func<Promise<RT>, RT>, ExceptionHandler {
			private Resolver<RT> mProxy;

			@Override
			public void run(Resolver<RT> resolve) {
				mProxy = resolve;
			}

			@Override
			public RT apply(Promise<RT> in) {
				if (in == null) {
					mProxy.resolve(null);
					return null;
				}
				
				in.then(new Func<RT,Void>() {					
					public Void apply(RT rt) {
						mProxy.resolve(rt);
						return null;
					}
				})
				.thenCatch(new Func<Throwable,Void>() {					
					public Void apply(Throwable t) {
						mProxy.reject(t);
						return null;
					}
				});
				return null;
			}

			@Override
			public void error(Throwable e) {
				if (mProxy != null) mProxy.reject(e);
			}
		};
		
		Continuation<OUT, Promise<RT>> cont = new Continuation<>(func);
		Promise<Promise<RT>> promise = new Promise<Promise<RT>>(cont);

		final Proxy proxy = new Proxy();
		promise.then(proxy)
		.thenCatch(new Func<Throwable,Void>() {
			
			@Override
			public Void apply(Throwable in) {
				proxy.error(in);
				return null;
			}
		});

		Promise<RT> rt = Promise.make(proxy);		
		mOut.addChild(cont);		
		return rt;
	}
	
	public <RT> Promise<RT> then( final Func<OUT, RT> func ) {
		Continuation<OUT, RT> cont = new Continuation<>(func);
		mOut.addChild(cont);			
		return new Promise<>(cont);
	}

//	public <RT> Promise<RT> thenOnUI(final Func<OUT, RT> func ) {
//		final Continuation<OUT, RT> cont = new Continuation<>(func);
//
//		class Proxy implements InComp<OUT>, Runnable {
//			OUT mIn;
//
//			@Override
//			public void apply(OUT in) {
//				mIn = in;
//				new android.os.Handler(Looper.getMainLooper()).post(this);
//			}
//
//			@Override
//			public void run() {
//				cont.apply(mIn);
//			}
//
//			@Override
//			public void error(Throwable e) {
//				cont.error(e);
//			}
//
//			@Override
//			public void doFinally() {
//				cont.doFinally();
//			}
//		};
//		mOut.addChild(new Proxy());
//		return new Promise<>(cont);
//	}
	
	public <RT> Promise<RT> thenAsync( final Func<OUT, RT> func ) {
		final Continuation<OUT, RT> cont = new Continuation<>(func);
		
		class Proxy implements InComp<OUT>, Runnable {
			OUT mIn;
			
			@Override
			public void apply(OUT in) {
				mIn = in;
				mPool.execute(this);
			}
			
			@Override
			public void run() {
				cont.apply(mIn);
			}

			@Override
			public void error(Throwable e) {
				cont.error(e);
			}

			@Override
			public void doFinally() {
				cont.doFinally();					
			}
		};
		mOut.addChild(new Proxy());
		return new Promise<>(cont);
	}
	
	public <RT> Promise<RT> thenCatch( Func<Throwable,RT> thenCatch, Func<Void,RT> thenFinally ) {
		thenCatch(thenCatch);
		return thenFinally(thenFinally);
		
		
//		final Continuation<Throwable, RT> cont = new Continuation<>(thenFinally);
//		mOut.setError(new ExceptionHandler() {
//			@Override
//			public void error(Throwable e) {
//				cont.apply(e);
//			}
//		});
//		return new Promise<>(cont);
	}
	
	public <RT> Promise<RT> thenCatch( Func<Throwable,RT> func ) {
		final Continuation<Throwable, RT> cont = new Continuation<>(func);
		mOut.setError(new ExceptionHandler() {
			@Override
			public void error(Throwable e) {
				cont.apply(e);
			}
		});
		return new Promise<>(cont);
	}
	
	public <RT> Promise<RT> thenFinally( Func<Void,RT> func ) {
		final Continuation<Void, RT> cont = new Continuation<>(func);
		mOut.setFinally(new FinallyHandler() {
			@Override
			public void doFinally() {
				cont.apply(null);
			}
		});
		return new Promise<>(cont);
	}
}

