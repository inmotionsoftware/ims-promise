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
import com.ims.tuple.Triplet;

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
	 * @param <OUT>
	 * @param <IN>
	 */
	public static interface IPromiseHandler<OUT,IN> extends IReject {
		public OUT resolve(IN in) throws Exception;
		public default void reject(Throwable t) {}	
		public default void finish() {}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	public static abstract class PromiseHandler<OUT,IN> implements IPromiseHandler<OUT, IN> {
		public void reject(Throwable t) {}
		public void finish() {}
	};
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 */
	public interface IPairCallback<OUT,A,B> extends IPromiseHandler<OUT, Pair<A,B>>  {
		public OUT resolve(A a, B b) throws Exception;
		public default OUT resolve(Pair<A,B> in) throws Exception {
			return resolve(in.get0(), in.get1());
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 */
	public static abstract class PairCallback<OUT,A,B> implements IPairCallback<OUT, A,B>  {		
		public OUT resolve(Pair<A,B> in) throws Exception {
			return resolve(in.get0(), in.get1());
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 */
	public interface ITripletCallback<OUT,A,B,C> extends IPromiseHandler<OUT, Triplet<A,B,C>>  {
		public OUT resolve(A a, B b, C c) throws Exception;
		public default OUT resolve(Triplet<A,B,C> in) throws Exception {
			return resolve(in.get0(), in.get1(), in.get2());
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 */
	public static abstract class TripletCallback<OUT,A,B,C> implements ITripletCallback<OUT,A,B,C>  {		
		public OUT resolve(Triplet<A,B,C> in) throws Exception {
			return resolve(in.get0(), in.get1(), in.get2());
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface IQuartetCallback<OUT,A,B,C,D> extends IPromiseHandler<OUT, Quartet<A,B,C,D>>  {
		public OUT resolve(A a, B b, C c, D d) throws Exception;
		public default OUT resolve(Quartet<A,B,C,D> in) throws Exception {
			return resolve(in.get0(), in.get1(), in.get2(), in.get3());
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public static abstract class QuartetCallback<OUT,A,B,C,D> implements IQuartetCallback<OUT,A,B,C,D>  {		
		public OUT resolve(Quartet<A,B,C,D> in) throws Exception {
			return resolve(in.get0(), in.get1(), in.get2(), in.get3());
		}
	}

	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public interface IResolver<IN> {	
		public void resolve(IN in);
		public void reject(Throwable e);
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public interface ICallback<IN> {
		void run(IResolver<IN> resolve);
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	public static interface IPromiseFunc<OUT,IN> extends IPromiseHandler<Promise<OUT>,IN> {
		public Promise<OUT> resolve(IN in) throws Exception;
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
		private final IPromiseHandler<OUT,IN> mCallback;
		
		protected Continuation(Result<OUT> res) {
			assert(res != null);
			mCallback = null;
			mResult = res;
		}
		
		protected Continuation( IPromiseHandler<OUT,IN> cb ) {
			mCallback = cb;
		}
		
		@Override
		public void resolve( Result<IN> result ) {
			if (result.error != null) {
				reject(result.error);
			} else {
				resolve(result.out);
			}
			mCallback.finish();
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

	private static Executor mMain;
	private static Executor mBack;
	private IOutComponent<OUT> mOut;

	static {
		int cores = Runtime.getRuntime().availableProcessors();
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		ThreadPoolExecutor exec = new ThreadPoolExecutor(2, cores, 10, TimeUnit.MINUTES, queue);
		setBackgroundExecutor(exec); // set the default
	}
	
	/**
	 * @param main
	 */
	public static void setMainExecutor(Executor main) {
		synchronized(Promise.class) {
			mMain = main;
		}
	}
	
	/**
	 * @param bg
	 */
	public static void setBackgroundExecutor(Executor bg) {
		synchronized(Promise.class) {
			mBack = bg;
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
		
		class PromiseList implements IPromiseHandler<Void,T>, ICallback<List<T>> {
			private List<T> list = new ArrayList<>();
			private AtomicInteger counter = new AtomicInteger(0);
			private IResolver<List<T>> mResolve;
			
			public void run(IResolver<List<T>> resolve) {
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
			
			public void finish() {
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
	public static <IN,OUT> Promise<OUT> async(final IN in, IPromiseHandler<OUT,IN> cb) {
		final Continuation<OUT,IN> cont = new Continuation<>(cb);

		mBack.execute(new Runnable() {				
			@Override
			public void run() {
				cont.resolve(new Result<IN>(in, null));
			}
		});
		return new Promise<>(cont);
	}

	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> async(IPromiseHandler<OUT,Void> cb) {
		return async(null, cb);
	}
	
	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> resolve(IPromiseHandler<OUT,Void> cb) {
		return resolve(null, cb);
	}

	/**
	 * @param in
	 * @param cb
	 * @return
	 */
	public static <OUT,IN> Promise<OUT> resolve(IN in, IPromiseHandler<OUT,IN> cb) {
		Continuation<OUT,IN> cont = new Continuation<>(cb);
		cont.resolve(new Result<>(in, null));
		return new Promise<>(cont);
	}

	/**
	 * @param in
	 * @return
	 */
	public static <INOUT> Promise<INOUT> resolve( INOUT in ) {
		// resolve immediately
		Result<INOUT> res = new Result<>(in, null);
		return new Promise<>(new Continuation<INOUT,INOUT>(res));
	}
	
	/**
	 * @param cb
	 * @return
	 */
	public static <OUT> Promise<OUT> make( ICallback<OUT> cb ) {
		final Continuation<OUT, OUT> cont = new Continuation<>(new IPromiseHandler<OUT,OUT>() {
			@Override
			public OUT resolve(OUT in) {
				return in;
			}
		});
		
		// wait for the callback to complete then forward the results to our continuation
		cb.run(new IResolver<OUT>() {
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
	public <RT> Promise<RT> then( final IPromiseFunc<RT,OUT> func ) {
		// This is a special case of a promise returning another promise
		
		// We create a promise "proxy" that will wait for the promise of the promise to be resolved then forward the
		// results
		final IPromiseHandler<Promise<RT>,OUT> base = func;
		return Promise.make(new ICallback<RT>() {

			@Override
			public void run(final IResolver<RT> promise) {
				
				// call the outer promise and wait for the promised result
				Promise.this.then(base).then(new IPromiseHandler<Void,Promise<RT>>() {
					@Override
					public Void resolve(Promise<RT> in) {
						
						// now we call the inner promise and forward the results
						in.then(new IPromiseHandler<Void,RT>() {
							@Override
							public Void resolve(RT in) {
								promise.resolve(in);
								return null;
							}	
							
							@Override
							public void reject(Throwable t) {
								promise.reject(t);
							}
						});
						return null;
					}
					
					@Override
					public void reject(Throwable t) {
						promise.reject(t);
					}
				});				
			}
		});
	}
	
	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> then( final IPromiseHandler<RT,OUT> func ) {
		Continuation<RT,OUT> cont = new Continuation<>(func);
		mOut.addChild(cont);			
		return new Promise<RT>(cont);
	}
	
	@SuppressWarnings("unchecked")
	public <RT,A,B> Promise<RT> then( final IPairCallback<RT,A,B> func ) {
		Continuation<RT,Pair<A,B>> cont = new Continuation<>(func);
		mOut.addChild((IInComponent<OUT>) cont);			
		return new Promise<RT>(cont);
	}
	
	@SuppressWarnings("unchecked")
	public <RT,A,B,C> Promise<RT> then( final ITripletCallback<RT,A,B,C> func ) {
		Continuation<RT,Triplet<A,B,C>> cont = new Continuation<>(func);
		mOut.addChild((IInComponent<OUT>) cont);			
		return new Promise<RT>(cont);
	}
	
	@SuppressWarnings("unchecked")
	public <RT,A,B,C,D> Promise<RT> then( final IQuartetCallback<RT,A,B,C,D> func ) {
		Continuation<RT,Quartet<A,B,C,D>> cont = new Continuation<>(func);
		mOut.addChild((IInComponent<OUT>) cont);			
		return new Promise<RT>(cont);
	}

	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> thenAsync( final IPromiseHandler<RT,OUT> func ) {
		final Continuation<RT,OUT> cont = new Continuation<>(func);
		mOut.addChild(new ThreadContinuationProxy<OUT>(cont, mBack));
		return new Promise<>(cont);
	}
	
	/**
	 * @param func
	 * @return
	 */
	public <RT> Promise<RT> thenOnMain( final IPromiseHandler<RT,OUT> func ) {
		// TODO: run on main thread somehow...
		final Continuation<RT,OUT> cont = new Continuation<>(func);
		mOut.addChild(new ThreadContinuationProxy<OUT>(cont, mMain));
		return new Promise<>(cont);
	}
	
	/**
	 * @param handler
	 * @return
	 */
	public Promise<Void> thenCatch(final IReject handler) {
		return this.then(new IPromiseHandler<Void,OUT>() {
			@Override
			public Void resolve(OUT in) { 
				return null; // we don't care about success results...
			}

			@Override
			public void reject(Throwable t) {
				handler.reject(t); // forward the error
			}			
		});
	}
}

