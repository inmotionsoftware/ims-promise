package com.ims.promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ims.tuple.Pair;
import com.ims.tuple.Quartet;
import com.ims.tuple.Quintet;
import com.ims.tuple.Triplet;
import com.ims.tuple.Unary;

/**
 * @author bghoward
 *
 * @param <OUT>
 */
public class Promise<OUT> {
	
	/**
	 * @author bghoward
	 *
	 */
	public static interface IReject {
		public void reject(Throwable t);
	}
	
	/**
	 * @author bghoward
	 *
	 */
	public static interface IAlways {
		public void always();
	}
	
	/**
	 * @author bghoward
	 *
	 */
	public static interface IResolve<OUT,IN> {
		public OUT resolve(IN in) throws Exception;
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	public static abstract class Handler<OUT,IN> implements IResolve<OUT,IN>, IReject, IAlways {
		public OUT resolve(IN in) throws Exception { return null; }
		public void always() {}
		public void reject(Throwable t) {}
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	public static abstract class PromiseHandler<OUT,IN> extends Handler<Promise<OUT>,IN> {
		public abstract Promise<OUT> resolve(IN in) throws Exception;
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 */
	public interface IUnaryCallback<OUT,A> {
		public OUT resolve(A a) throws Exception;
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 */
	public interface IPairCallback<OUT,A,B> {
		public OUT resolve(A a, B b) throws Exception;
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 */
	public interface ITripletCallback<OUT,A,B,C> {
		public OUT resolve(A a, B b, C c) throws Exception;
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface IQuartetCallback<OUT,A,B,C,D> {
		public OUT resolve(A a, B b, C c, D d) throws Exception;
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param <E>
	 */
	public interface IQuintetCallback<OUT,A,B,C,D,E> {
		public OUT resolve(A a, B b, C c, D d, E e) throws Exception;
	}

	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public static interface IDeferred<IN> {	
		public void resolve(IN in);
		public void reject(Throwable e);
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public static interface Deferrable<IN> {
		void run(IDeferred<IN> resolve);
	}

	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 */
	private static class ThreadContinuationProxy<OUT> implements IInComponent<OUT>, Runnable {
		final IInComponent<OUT> mFwd;
		Result<OUT> mResult;
		Executor mExecutor;
		
		ThreadContinuationProxy( IInComponent<OUT> fwd, Executor exe ) {
			mFwd = fwd;
			mExecutor = exe;
		}

		@Override
		public void run() {
			assert(mResult != null);
			mFwd.resolve(mResult);
		}

		@Override
		public void resolve(Result<OUT> result) {
			
			// error occurred send it along, no need to spawn on another thread
			if (result.error != null) {
				mFwd.resolve(result);
				return;
			}

			// process the results in a background thread...
			mResult = result; // assign the results	
			mExecutor.execute(this);
		}
	};

	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 */
	private static class Result<OUT> {
		public OUT out;
		public Throwable error;
		public Result(OUT o, Throwable err) {
			this.out = o;
			this.error = err;
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	private interface IInComponent<IN> {
		public void resolve(Result<IN> result);
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 */
	private interface IOutComponent<OUT> {
		public void addChild( IInComponent<OUT> child );
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	private static class Continuation<OUT,IN> implements IOutComponent<OUT>, IInComponent<IN> {

		private List< IInComponent<OUT> > mChildren;
		private Result<OUT> mResult;
		private final Handler<OUT,IN> mCallback;
		
		protected Continuation(Result<OUT> res) {
			assert(res != null);
			mCallback = null;
			mResult = res;
		}
		
		protected Continuation( Handler<OUT,IN> cb ) {
			mCallback = cb;
		}
		
		@Override
		public void resolve( Result<IN> result ) {
			if (result.error != null) {
				reject(result.error);
			} else {
				resolve(result.out);
			}
			mCallback.always();
		}
		
		private void resolve(IN in) {
			Result<OUT> result;
			try {
				OUT out = mCallback.resolve(in);
				result = new Result<OUT>(out, null);
			} catch (Exception e) {
				result = new Result<OUT>(null, e);
			}			
			setResult(result);
		}
		
		private void reject(Throwable t) {
			try {
				mCallback.reject(t);
			} catch(Exception e) {}
			setResult(new Result<OUT>(null, t));
		}
		
		private void setResult(Result<OUT> result) {
			List< IInComponent<OUT> > children = null;
			synchronized (this) {
				mResult = result;
				children = mChildren;
				mChildren = null;
			}
			
			if (children == null) return;
			
			for (IInComponent<OUT> child : children) {
				child.resolve(result);
			}
		}

		@Override
		public void addChild(IInComponent<OUT> child) {
			
			Result<OUT> result;
			
			// check the results to see if this promise is already resolved or not. This needs to be synchronized for
			// thread access.
			synchronized (this) {
				result = mResult;
				if (mResult == null) {
					// first child, lazy creation of list
					if (mChildren == null) mChildren = new ArrayList<>();
					mChildren.add(child);					
				}
			}
			
			// this promise has already been resolved, go ahead and process the results!
			if (result != null) child.resolve(result);
		}		
	}

	private static Executor gMain;
	private static Executor gBack;

	private static Executor getMain() {
		return gMain;
	}
	
	private static Executor getBG() {
		synchronized(Promise.class) {
			if (gBack == null) {
				int cores = Runtime.getRuntime().availableProcessors();
				BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
				ThreadPoolExecutor exec = new ThreadPoolExecutor(2, cores, 10, TimeUnit.MINUTES, queue);
				setBackgroundExecutor(exec); // set the default
			}
			return gBack;
		}
	}
	
	
	private IOutComponent<OUT> mOut;

	
	/**
	 * @param main
	 */
	public static void setMainExecutor(Executor main) {
		synchronized(Promise.class) {
			gMain = main;
		}
	}
	
	/**
	 * @param bg
	 */
	public static void setBackgroundExecutor(Executor bg) {
		synchronized(Promise.class) {
			gBack = bg;
		}
	}

	/**
	 * @param out
	 */
	private Promise(IOutComponent<OUT> out) {
		mOut = out;
	}
	
	/**
	 * @param iter
	 * @return
	 */
	public static<T> Promise<List<T>> all(final Iterable<Promise<T>> iter) {
		
		class PromiseList extends Handler<Void,T> implements Deferrable<List<T>> {
			private List<T> list = new ArrayList<>();
			private AtomicInteger counter = new AtomicInteger(0);
			private IDeferred<List<T>> mResolve;
			
			@Override
			public void run(IDeferred<List<T>> resolve) {
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
			public void always() {
				decrement();
			}

			@Override
			public Void resolve(T in) {
				synchronized (this) {
					list.add(in);	
				}
				return null;
			}

			@Override
			public void reject(Throwable t) {} // TODO: how to return errors?
		};
		
		return Promise.make(new PromiseList());
	}
	
	/**
	 * @param in
	 * @param func
	 * @return
	 */
	public static <IN,OUT> Promise<OUT> main(final IN in, Handler<OUT,IN> cb) {
		return resolve(in, cb, getMain());
	}

	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> main(Handler<OUT,Void> cb) {
		return resolve(null, cb, getMain());
	}
	
	/**
	 * @param in
	 * @param func
	 * @return
	 */
	public static <IN,OUT> Promise<OUT> main(final IN in, IResolve<OUT,IN> cb) {
		return resolve(in, cb, getMain());
	}

	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> main(IResolve<OUT,Void> cb) {
		return resolve(null, cb, getMain());
	}
	
	/**
	 * @param in
	 * @param func
	 * @return
	 */
	public static <IN,OUT> Promise<OUT> async(final IN in, IResolve<OUT,IN> cb) {
		return resolve(in, cb, getBG());
	}

	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> async(IResolve<OUT,Void> cb) {
		return resolve(null, cb, getBG());
	}
	
	/**
	 * @param in
	 * @param func
	 * @return
	 */
	public static <IN,OUT> Promise<OUT> async(final IN in, Handler<OUT,IN> cb) {
		return resolve(in, cb, getBG());
	}

	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> async(Handler<OUT,Void> cb) {
		return resolve(null, cb, getBG());
	}

	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> resolve(IResolve<OUT,Void> cb) {
		return resolve(null, cb, null);
	}
	
	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> resolve(Handler<OUT,Void> cb) {
		return resolve(null, cb, null);
	}
	
	/**
	 * @param in
	 * @param cb
	 * @return
	 */
	public static <OUT,IN> Promise<OUT> resolve(IN in, IResolve<OUT,IN> cb) {
		return resolve(in, cb, null);
	}

	/**
	 * @param in
	 * @param cb
	 * @return
	 */
	public static <OUT,IN> Promise<OUT> resolve(IN in, Handler<OUT,IN> cb) {
		return resolve(in, cb, null);
	}
	
	/**
	 * 
	 * @param in
	 * @param cb
	 * @param exe
	 * @return
	 */
	public static <OUT,IN> Promise<OUT> resolve(IN in, IResolve<OUT,IN> cb, Executor exe) {
		return resolve(in, new Handler<OUT, IN>() {
			@Override
			public OUT resolve(IN in) throws Exception {
				return cb.resolve(in);
			}
		}, exe);
	}
	
	/**
	 * 
	 * @param in
	 * @param cb
	 * @param exe
	 * @return
	 */
	public static <OUT,IN> Promise<OUT> resolve(IN in, Handler<OUT,IN> cb, Executor exe) {
		final Continuation<OUT,IN> cont = new Continuation<>(cb);
		if (exe != null) {
			ThreadContinuationProxy<IN> proxy = new ThreadContinuationProxy<>(cont, exe);
			proxy.resolve(new Result<>(in, null));
		} else {
			cont.resolve(new Result<>(in, null));
		}
		return new Promise<>(cont);
	}

	/**
	 * @param in
	 * @return
	 */
	public static <IN> Promise<IN> resolve( IN in ) {
		// resolve immediately
		Result<IN> res = new Result<>(in, null);
		return new Promise<>(new Continuation<IN,IN>(res));
	}
	
	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> make( Deferrable<OUT> cb ) {
		final Continuation<OUT, OUT> cont = new Continuation<>(new Handler<OUT,OUT>() {
			@Override
			public OUT resolve(OUT in) {
				return in;
			}
		});
		
		// wait for the callback to complete then forward the results to our continuation
		cb.run(new IDeferred<OUT>() {
			@Override
			public void resolve(OUT in) {
				// success!!
				cont.resolve(new Result<OUT>(in,null));
			}

			@Override
			public void reject(Throwable t) {
				// failure!!
				cont.resolve(new Result<OUT>(null,t));
			}
		});
		return new Promise<>(cont);
	}
	
	
	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> then( final PromiseHandler<RT,OUT> func ) {
		// This is a special case of a promise returning another promise
		
		// We create a promise "proxy" that will wait for the promise of the promise to be resolved then forward the
		// results
		final Handler<Promise<RT>,OUT> base = func;
		return Promise.make(new Deferrable<RT>() {
			@Override
			public void run(final IDeferred<RT> promise) {
				
				// call the outer promise and wait for the promised result
				Promise.this.then(base).then(new Handler<Void,Promise<RT>>() {
					@Override
					public Void resolve(Promise<RT> in) {
						
						// now we call the inner promise and forward the results
						in.then(new Handler<Void,RT>() {
							@Override
							public Void resolve(RT in) {
								promise.resolve(in);
								return null;
							}	
							
							@Override
							public void reject(Throwable t) {
								promise.reject(t);
							}
							
							@Override
							public void always() {}
						});
						return null;
					}
					
					@Override
					public void reject(Throwable t) {
						promise.reject(t);
					}
					
					@Override
					public void always() {}
				});				
			}
		});
	}
	
	/**
	 * 
	 * @param handler
	 * @param exe
	 * @return
	 */
	public <RT> Promise<RT> then( final Handler<RT, OUT> handler, Executor exe ) {
		final Continuation<RT,OUT> cont = new Continuation<>(handler);
		if (exe != null) {
			mOut.addChild(new ThreadContinuationProxy<OUT>(cont, exe));
		} else {
			mOut.addChild(cont);
		}
		return new Promise<>(cont);
	}	
	
	/**
	 * 
	 * @param handler
	 * @return
	 */
	public <RT> Promise<RT> then(final IResolve<RT, OUT> handler, Executor exe ) {
		return this.then(new Handler<RT,OUT>() {
			@Override
			public RT resolve(OUT in) throws Exception { return handler.resolve(in); }
		}, exe);
	}

	/**
	 * @param cb
	 * @return
	 */
	public <RT> Promise<RT> then( final Handler<RT,OUT> cb ) {
		return this.then(cb, null);
	}

	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> thenAsync( final Handler<RT,OUT> func ) {
		return then(func, getBG());
	}
	
	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> thenOnMain( final Handler<RT,OUT> func ) {
		return then(func, getMain());
	}
	
	/**
	 * @param cb
	 * @return
	 */
	public <RT> Promise<RT> then( final IResolve<RT,OUT> cb ) {
		return this.then(cb, null);
	}

	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> thenAsync( final IResolve<RT,OUT> func ) {
		return then(func, getBG());
	}
	
	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> thenOnMain( final IResolve<RT,OUT> func ) {
		return then(func, getMain());
	}

	/**
	 * @param handler
	 * @return
	 */
	public Promise<Void> always(final IAlways handler) {
		return this.then(new Handler<Void,OUT>() {			
			@Override
			public void always() { handler.always(); }
		});
	}
	
	/**
	 * @param handler
	 * @return
	 */
	public Promise<Void> fail(final IReject handler) {
		return this.then(new Handler<Void,OUT>() {
			@Override
			public void reject(Throwable t) { handler.reject(t); }
		});
	}
	
	/**
	 * 
	 * @param cb
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <RT,A> Promise<RT> then( final IUnaryCallback<RT,A> cb ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Unary<A>>() {
			@Override
			public RT resolve(Unary<A> in) throws Exception {
				return cb.resolve(in.get0());
			}
		}, null);
	}
	
	/**
	 * 
	 * @param cb
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B> Promise<RT> then( final IPairCallback<RT,A,B> cb ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Pair<A,B>>() {
			@Override
			public RT resolve(Pair<A,B> in) throws Exception {
				return cb.resolve(in.get0(), in.get1());
			}
		}, null);
	}
	
	/**
	 * 
	 * @param cb
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B,C> Promise<RT> then( final ITripletCallback<RT,A,B,C> cb ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Triplet<A,B,C>>() {
			@Override
			public RT resolve(Triplet<A,B,C> in) throws Exception {
				return cb.resolve(in.get0(), in.get1(), in.get2());
			}
		}, null);
	}
	
	/**
	 * 
	 * @param cb
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B,C,D> Promise<RT> then( final IQuartetCallback<RT,A,B,C,D> cb ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Quartet<A,B,C,D>>() {
			@Override
			public RT resolve(Quartet<A,B,C,D> in) throws Exception {
				return cb.resolve(in.get0(), in.get1(), in.get2(), in.get3());
			}
		}, null);
	}
	
	/**
	 * 
	 * @param cb
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B,C,D,E> Promise<RT> then( final IQuintetCallback<RT,A,B,C,D,E> cb ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Quintet<A,B,C,D,E>>() {
			@Override
			public RT resolve(Quintet<A,B,C,D,E> in) throws Exception {
				return cb.resolve(in.get0(), in.get1(), in.get2(), in.get3(), in.get4());
			}
		}, null);
	}

}

