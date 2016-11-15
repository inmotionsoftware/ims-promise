package com.inmotionsoftware.promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.inmotionsoftware.tuple.Pair;
import com.inmotionsoftware.tuple.Quartet;
import com.inmotionsoftware.tuple.Quintet;
import com.inmotionsoftware.tuple.Triplet;

/**
 * @author bghoward
 *
 * @param <OUT>
 */
public class Promise<OUT> {
	
	public static final String VERSION = "0.1.4"; 
	
	/**
	 * @author bghoward
	 *
	 */
	public interface IReject {
		void reject(Throwable t);
	}
	
	/**
	 * @author bghoward
	 *
	 */
	public interface IAlways {
		void always();
	}
	
	/**
	 * @author bghoward
	 *
	 */
	public interface IResolve<OUT,IN> {
		OUT resolve(IN in) throws Exception;
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public interface VoidResolve<IN> {
		void resolve(IN in) throws Exception;
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 */
	public interface ResolveVoid<OUT> {
		OUT resolve() throws Exception;
	}
	
	/**
	 * @author bghoward
	 *
	 */
	public interface VoidResolveVoid {
		void resolve() throws Exception;
	}
	
	/**
	 * @author bghoward
	 *
	 */
	public interface IPromiseResolve<OUT,IN> extends IResolve<Promise<OUT>,IN> {
		@Override
		Promise<OUT> resolve(IN in) throws Exception;
	}

	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	public static abstract class Handler<OUT,IN> implements IResolve<OUT,IN>, IReject, IAlways {
		@Override
		public abstract OUT resolve(IN in) throws Exception;
		@Override
		public void reject(Throwable t) {}
		@Override
		public void always() {}
	}

    private static class ProxyHandler<OUT,IN> extends Handler<OUT,IN> {

        private IResolve<OUT,IN> mResolve;
        private IReject mReject;
        private IAlways mAlways;

        ProxyHandler(IResolve<OUT,IN> resolve, IReject reject, IAlways always) {
            mResolve = resolve;
            mReject = reject;
            mAlways = always;
        }

        @Override
        public OUT resolve(IN in) throws Exception {
            if (mResolve != null) return mResolve.resolve(in);
            return null;
        }

        @Override
        public void reject(Throwable t) {
            if (mReject != null) mReject.reject(t);
        }

        @Override
        public void always() {
            if (mAlways != null) mAlways.always();
        }
    }

	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	private static class FailHandler<IN> extends Handler<Void,IN> {
		private final IReject mChild;
		
		FailHandler(IReject child) {
			if (child == null) throw new NullPointerException("FailHandler must have a child handler");
			mChild = child;
		}
		
		@Override
		public Void resolve(IN in) throws Exception { return null; }
		
		@Override
		public void reject(Throwable t) {
			mChild.reject(t);
		}
	}
	
	/**
	 * 
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	public static abstract class PromiseHandler<OUT,IN> extends Handler<Promise<OUT>,IN> implements IPromiseResolve<OUT,IN> {
		@Override
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
		OUT resolve(A a) throws Exception;
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
		OUT resolve(A a, B b) throws Exception;
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
		OUT resolve(A a, B b, C c) throws Exception;
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
		OUT resolve(A a, B b, C c, D d) throws Exception;
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
		OUT resolve(A a, B b, C c, D d, E e) throws Exception;
	}

	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public interface IDeferred<IN> {
		void resolve(IN in);
		void reject(Throwable e);
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	public interface Deferrable<IN> {
		void run(IDeferred<IN> resolve) throws Exception;
	}


	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 */
	private static class Result<OUT> {
		final OUT out;
		final Throwable error;
		Result(OUT o, Throwable err) {
			this.out = o;
			this.error = err;
		}
	}

    /**
     *
     * @param <T> Promises of type T
     */
	public final static class AggregateResults<T> {
		private final List<T> mSucceeded = new ArrayList<>();
		private final List<Throwable> mFailed = new ArrayList<>();

        private AggregateResults() {}
		
		void success(T suc) {
			synchronized(mSucceeded) {
				mSucceeded.add(suc);				
			}
		}
		
		synchronized void failed(Throwable t) {
			synchronized(mFailed) {
				mFailed.add(t);
			}
		}
		
		public List<T> getSuccesses() {
            return mSucceeded;
        }
		public List<Throwable> getFailures() {
			return mFailed;
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <IN>
	 */
	private interface IInComponent<IN> {
		void resolve(Result<IN> result);
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 */
	private interface IOutComponent<OUT> {
		void addChild( IInComponent<OUT> child );
		Executor getExecutor();
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	private static abstract class BaseContinuation<OUT,IN> implements IOutComponent<OUT>, IInComponent<IN> {

		private List< IInComponent<OUT> > mChildren;
		private Result<OUT> mResult;
		private final Executor mExecutor;
		
		BaseContinuation(Executor exe) {
			mExecutor = exe;
		}
		
		@Override
		public void resolve( final Result<IN> result ) {
			runWithExecutor(new Runnable() {
				@Override
				public void run() {
					if (result.error != null) {
						reject(result.error);
					} else {
						resolve(result.out);	
					}
				}
			});
		}
		
		void runWithExecutor(Runnable r) {
			if (mExecutor != null) {
				mExecutor.execute(r);
			} else {
				r.run();
			}
		}
		
		protected abstract void resolve(IN in);
		protected abstract void reject(Throwable t);
		
		void dispatchResult(Result<OUT> result) {

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
		
		public Executor getExecutor() {
			return mExecutor;
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <T>
	 */
	private static class ResolvedContinuation<T> extends BaseContinuation<T,T> {
		ResolvedContinuation(T res, Throwable t) {
			super(null);
			super.mResult = new Result<T>(res, t);
		}
		
		@Override
		protected void resolve(T in) {
			dispatchResult(super.mResult);
		}

		@Override
		protected void reject(Throwable t) {
			dispatchResult(super.mResult);
		}
	}
	
	/**
	 * @author bghoward
	 *
	 * @param <T>
	 */
	private static class DeferredContinuation<T> extends BaseContinuation<T,T> {
		
		private final Deferrable<T> mDeferrable;
		
		DeferredContinuation( Deferrable<T> d, Executor exe ) {
			super(exe);
			mDeferrable = d;
		}

		void start() {
			runWithExecutor(new Runnable() {
				@Override
				public void run() {
					dispatch();
				}
			});
		}

		private void dispatch() {
			try {
				mDeferrable.run(new IDeferred<T>() {
					@Override
					public void resolve(T in) {
						DeferredContinuation.this.resolve(in);
					}
	
					@Override
					public void reject(Throwable e) {
						DeferredContinuation.this.reject(e);
					}
				});				
			} catch (Throwable e) {
				DeferredContinuation.this.reject(e);
			}
		}
		
		@Override
		protected void resolve(T in) {
			dispatchResult(new Result<T>(in, null));
		}

		@Override
		protected void reject(Throwable t) {
			dispatchResult(new Result<T>(null, t));
		}
	}

	/**
	 * @author bghoward
	 *
	 * @param <OUT>
	 * @param <IN>
	 */
	private static class CallbackContinuation<OUT,IN> extends BaseContinuation<OUT,IN> {

		private final Handler<OUT,IN> mCallback;
		
		CallbackContinuation( Handler<OUT,IN> cb, Executor exe) {
			super(exe);
			if (cb == null) throw new NullPointerException("CallbackContinuation cannot have a null callback");
			mCallback = cb;
		}
		
		@Override
		protected void resolve(IN in) {
			Result<OUT> result;
			try {				
				OUT out = mCallback.resolve(in);
				result = new Result<>(out, null);
			} catch (Throwable e) {
				result = new Result<>(null, e);
			}
			dispatchResult(result);
			mCallback.always();
		}

		@Override
		protected void reject(Throwable t) {
            try {
                try {
                    mCallback.reject(t);
                } catch(Throwable e) {
                    // ignore: we don't want the handler to cause our promise chain to stop
                }

                // decide if we should forward the error, or do we suppress it...
                Throwable fwd = (mCallback instanceof FailHandler) ? null : t;
                dispatchResult(new Result<OUT>(null, fwd));

            } finally {
                mCallback.always(); // we should always notify
            }
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
	 * @param main the executor for the main thread
	 */
	public static void setMainExecutor(Executor main) {
		synchronized(Promise.class) {
			gMain = main;
		}
	}
	
	/**
	 * @param bg the executor for background threads
	 */
	public static void setBackgroundExecutor(Executor bg) {
		synchronized(Promise.class) {
			gBack = bg;
		}
	}

	/**
	 * @param out the previous promise in the chain
	 */
	private Promise(IOutComponent<OUT> out) {
		mOut = out;
	}
	
	/**
	 * @param iter iterable list of promises
	 * @return a promise
	 */
	public static<T> Promise<AggregateResults<T>> all(final Iterable<Promise<T>> iter) {
		
		class AllHandler extends Handler<Void,T> {
			
			private final AggregateResults<T> mResults = new AggregateResults<>();
			private AtomicInteger mCount = new AtomicInteger(0);
			private IDeferred<AggregateResults<T>> mResolve;						

			private void attach(IDeferred<AggregateResults<T>> resolve) {				
				mResolve = resolve;
				increment();
				for (Promise<T> promise : iter) {
					increment();
					promise.then(this);
				}
				decrement();
			}
			
			private void increment() {
				mCount.incrementAndGet();
			}

			private void decrement() {
				int count = mCount.decrementAndGet();
				if (count == 0) complete();
			}
			
			private void complete() {
				mResolve.resolve(mResults);
			}
			
			@Override
			public void always() {
				decrement();
			}

			@Override
			public Void resolve(T in) {
				mResults.success(in);
				return null;
			}

			@Override
			public void reject(Throwable t) {
				mResults.failed(t);
			} 
		};

		final AllHandler all = new AllHandler();
		return Promise.make(new Deferrable<AggregateResults<T>>() {
			@Override
			public void run(IDeferred<AggregateResults<T>> resolve) throws Exception {
				all.attach(resolve);
			}
		});
	}

	/**
	 * @param in the item to resolve this promise with
	 * @return a resolved promise
	 */
	public static <IN> Promise<IN> resolve( IN in ) {
		return new Promise<>(new ResolvedContinuation<IN>(in, null));
	}
	
	/**
	 * 
	 * @return a resolved promise of type Void
	 */
	public static Promise<Void> resolve() {
		return resolve(null);
	}
	
	/**
	 * @param t the error to reject this promise with
	 * @return a new promise rejected with an error
	 */
	public static <T> Promise<T> reject(Throwable t) {
		return new Promise<T>(new ResolvedContinuation<T>(null, t));
	}
	
	/**
	 * @param def the deferrable this promise will be resolved with
	 * @return a new Promise to be resolved by the Deferrable
	 */
	public static <OUT> Promise<OUT> makeAsync( Deferrable<OUT> def) {
		return make(def, getBG());
	}
	
	/**
     * @param def the deferrable this promise will be resolved with
     * @return a new Promise to be resolved by the Deferrable
	 */
	public static <OUT> Promise<OUT> makeOnMain( Deferrable<OUT> def) {
		return make(def, getMain());
	}
	
	/**
     * @param def the deferrable this promise will be resolved with
     * @return a new Promise to be resolved by the Deferrable
	 */
	public static <OUT> Promise<OUT> make( Deferrable<OUT> def, Executor exe ) {
		DeferredContinuation<OUT> cont = new DeferredContinuation<>(def, exe);
		cont.start();
		return new Promise<>(cont);
	}
	
	/**
     * @param def the deferrable this promise will be resolved with
     * @return a new Promise to be resolved by the Deferrable
	 */
	public static <OUT> Promise<OUT> make( Deferrable<OUT> def ) {
		return make(def, null);
	}
	
	/**
	 * @param handler the handler for the next promise in the chain
	 * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final PromiseHandler<RT,OUT> handler ) {
		return then(handler, (Executor)null);
	}
	
	/**
	 * 
	 * @param handler the handler for the next promise in the chain
	 * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final IResolve<RT, OUT> handler, Executor exe ) {
        return this.then(new ProxyHandler<>(handler, null, null), exe);
    }
	
	/**
	 * @param handler the handler for the next promise in the chain
	 * @return a new promise chained by this one
	 */
	public Promise<Void> then( final VoidResolve<OUT> handler ) {
		return this.then(handler, null);
	}
	
	/**
	 * @param handler the handler for the next promise in the chain
	 * @param exe the executor that will process this promise
	 * @return a new promise chained by this one
	 */
	public Promise<Void> then( final VoidResolve<OUT> handler, Executor exe ) {
		return this.then(new Handler<Void,OUT>() {
			@Override
			public Void resolve(OUT in) throws Exception { handler.resolve(in); return null; }
		}, exe);
	}

	/**
	 * @param handler the handler for the next promise in the chain
	 * @return a new promise chained by this one
	 */
	public Promise<Void> then( final VoidResolveVoid handler ) {
		return then(handler, null);
	}
	
	/**
	 * @param handler the handler for the next promise in the chain
	 * @param exe the executor that will process this promise
     * @return a new promise chained by this one
	 */
	public Promise<Void> then( final VoidResolveVoid handler, Executor exe ) {
		return this.then(new Handler<Void,OUT>() {
			@Override
			public Void resolve(OUT in) throws Exception { handler.resolve(); return null; }
		}, exe);
	}
	
	
	/**
	 * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final ResolveVoid<RT> handler ) {
		return this.then(handler, null);
	}

	/**
     * @param handler the handler for the next promise in the chain
     * @param exe the executor that will process this promise
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final ResolveVoid<RT> handler, Executor exe ) {
		return this.then(new Handler<RT,OUT>() {
			@Override
			public RT resolve(OUT in) throws Exception { return handler.resolve(); }
		}, exe);
	}

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @param always notified regardless of the outcome
     * @param exe the executor that will process this promise
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> then( IResolve<RT,OUT> resolve, IReject reject, IAlways always, Executor exe ) {
        return then(new ProxyHandler<>(resolve, reject, always), exe);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> then( IResolve<RT,OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> then( IResolve<RT,OUT> resolve, IReject reject) {
        return then(resolve, reject, null);
    }

	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final Handler<RT,OUT> cb ) {
		return this.then(cb, (Executor)null);
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final IResolve<RT,OUT> cb ) {
		return this.then(cb, (Executor)null);
	}

	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final IPromiseResolve<RT,OUT> cb ) {
		return this.then(cb, (Executor)null);
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final IPromiseResolve<RT,OUT> cb, Executor exe ) {
		return this.then(new PromiseHandler<RT, OUT>() {
			@Override
			public Promise<RT> resolve(OUT in) throws Exception {
				return cb.resolve(in);
			}
		}, exe);
	}
	
	/**
     * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( final PromiseHandler<RT,OUT> handler, final Executor exe ) {
		// This is a special case of a promise returning another promise
		
		// We create a promise "proxy" that will wait for the promise of the promise to be resolved then forward the
		// results
		final Promise<Promise<RT>> inner = this.then((Handler<Promise<RT>,OUT>)handler, exe);
		
		return Promise.make(new Deferrable<RT>() {
			@Override
			public void run(final IDeferred<RT> promise) {
				
				// call the outer promise and wait for the promised result
				inner.then(new Handler<Void,Promise<RT>>() {

					@Override
					public Void resolve(Promise<RT> in) {
						
						if (in == null) {
							promise.reject(new NullPointerException());
//							promise.resolve(null);
							return null;
						}
						
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
						}, exe);
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
		}, exe);
	}
	
	/**
	 *
     * @param handler the handler for the next promise in the chain
     * @param exe the executor that will process this promise
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> then( Handler<RT, OUT> handler, Executor exe ) {
		if (exe == null) { // inherit from the parent
			exe = mOut.getExecutor();
		}

		CallbackContinuation<RT,OUT> child = new CallbackContinuation<>(handler, exe);
		mOut.addChild(child);
		return new Promise<>(child);
	}

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> thenAsync( IResolve<RT,OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getBG());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> thenAsync( IResolve<RT,OUT> resolve, IReject reject) {
        return thenAsync(resolve, reject, null);
    }

	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenAsync( IPromiseResolve<RT,OUT> cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenAsync( IResolve<RT,OUT> cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public Promise<Void> thenAsync( VoidResolve<OUT> cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenAsync( ResolveVoid<RT> cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public Promise<Void> thenAsync( VoidResolveVoid cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenAsync( Handler<RT,OUT> cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenAsync( PromiseHandler<RT,OUT> cb ) {
		return then(cb, getBG());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public Promise<Void> thenOnMain( VoidResolve<OUT> cb ) {
		return then(cb, getMain());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenOnMain( ResolveVoid<RT> cb ) {
		return then(cb, getMain());
	}

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> thenOnMain( IResolve<RT,OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getMain());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT,IN> Promise<RT> thenOnMain( IResolve<RT,OUT> resolve, IReject reject) {
        return thenAsync(resolve, reject, null);
    }
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public Promise<Void> thenOnMain( VoidResolveVoid cb ) {
		return then(cb, getMain());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenOnMain( PromiseHandler<RT,OUT> cb ) {
		return then(cb, getMain());
	}	
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenOnMain( IPromiseResolve<RT,OUT> cb ) {
		return then(cb, getMain());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenOnMain( IResolve<RT,OUT> cb ) {
		return then(cb, getMain());
	}
	
	/**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT> Promise<RT> thenOnMain( Handler<RT,OUT> cb ) {
		return then(cb, getMain());
	}
	
	/**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
	 */
	public Promise<Void> alwaysAsync( IAlways always ) {
        return this.always(always, getBG());
	}

	/**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
	 */
	public Promise<Void> alwaysOnMain( IAlways always ) {
		return this.always(always, getMain());
	}
	
	/**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
	 */
	public Promise<Void> always( IAlways always ) {
		return this.always(always, null);
	}

    /**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> always( IAlways always, Executor exe ) {
        return this.then(null, null, always, exe);
    }

	/**
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
	 */
	public Promise<Void> failOnMain(final IReject reject) {
		return this.fail(reject, getMain());
	}
	
	/**
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
	 */
	public Promise<Void> fail(final IReject reject) {
		return this.fail(reject, null);
	}
	
	/**
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
	 */
	public Promise<Void> failAsync(final IReject reject) {
		return this.fail(reject, getBG());
	}
	
	/**
     * @param reject notified if the promise is rejected
     * @param exe the executor that will process this promise
     * @return a new promise chained by this one
	 */
	public Promise<Void> fail(final IReject reject, Executor exe) {
		return this.then(new FailHandler<OUT>(reject), exe);
	}

    /**
     *
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> failAsyncThenAlways(final IReject reject) {
        return failThenAlways(reject, getBG());
    }

    /**
     *
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> failOnMainThenAlways(final IReject reject) {
        return failThenAlways(reject, getMain());
    }

    /**
     *
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> failThenAlways(final IReject reject) {
        return failThenAlways(reject, null);
    }

    /**
     *
     * @param reject
     * @param exe
     * @return
     */
    @SuppressWarnings("unchecked")
    public Promise<Void> failThenAlways(final IReject reject, final Executor exe) {

        return Promise.make(new Deferrable<Void>() {
            @Override
            public void run(final IDeferred<Void> def) throws Exception {
                then(new Handler<Void, OUT>() {
                    @Override
                    public Void resolve(OUT out) throws Exception { return null; }

                    @Override
                    public void always() {
                        def.resolve(null);
                    }
                }, exe);
            }
        });
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    @SuppressWarnings("unchecked")
    public <RT,A,B> Promise<RT> then( final IPairCallback<RT,A,B> cb, Executor exe ) {
        return this.then((Handler<RT,OUT>) new Handler<RT, Pair<A,B>>() {
            @Override
            public RT resolve(Pair<A,B> in) throws Exception {
                return cb.resolve(in.get0(), in.get1());
            }
        }, exe);
    }
	
	/**
	 *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	public <RT,A,B> Promise<RT> then( IPairCallback<RT,A,B> cb ) {
		return this.then(cb, null);
	}

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B> Promise<RT> thenAsync( IPairCallback<RT,A,B> cb ) {
        return this.then(cb, getBG());
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B> Promise<RT> thenOnMain( IPairCallback<RT,A,B> cb ) {
        return this.then(cb, getMain());
    }
	
	/**
	 *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B,C> Promise<RT> then( final ITripletCallback<RT,A,B,C> cb, Executor exe ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Triplet<A,B,C>>() {
			@Override
			public RT resolve(Triplet<A,B,C> in) throws Exception {
				return cb.resolve(in.get0(), in.get1(), in.get2());
			}
		}, exe);
	}

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C> Promise<RT> then( ITripletCallback<RT,A,B,C> cb) {
        return then(cb, null);
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C> Promise<RT> thenAsync( ITripletCallback<RT,A,B,C> cb) {
        return then(cb, getBG());
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C> Promise<RT> thenOnMain( ITripletCallback<RT,A,B,C> cb) {
        return then(cb, getMain());
    }
	
	/**
	 *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B,C,D> Promise<RT> then( final IQuartetCallback<RT,A,B,C,D> cb, Executor exe  ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Quartet<A,B,C,D>>() {
			@Override
			public RT resolve(Quartet<A,B,C,D> in) throws Exception {
				return cb.resolve(in.get0(), in.get1(), in.get2(), in.get3());
			}
		}, exe);
	}

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D> Promise<RT> then( IQuartetCallback<RT,A,B,C,D> cb) {
        return then(cb, null);
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D> Promise<RT> thenAsync( IQuartetCallback<RT,A,B,C,D> cb) {
        return then(cb, getBG());
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D> Promise<RT> thenOnMain( IQuartetCallback<RT,A,B,C,D> cb) {
        return then(cb, getMain());
    }
	
	/**
	 *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
	 */
	@SuppressWarnings("unchecked")
	public <RT,A,B,C,D,E> Promise<RT> then( final IQuintetCallback<RT,A,B,C,D,E> cb, Executor exe ) {
		return this.then((Handler<RT,OUT>) new Handler<RT, Quintet<A,B,C,D,E>>() {
			@Override
			public RT resolve(Quintet<A,B,C,D,E> in) throws Exception {
				return cb.resolve(in.get0(), in.get1(), in.get2(), in.get3(), in.get4());
			}
		}, exe);
	}

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D,E> Promise<RT> then(IQuintetCallback<RT,A,B,C,D,E> cb ) {
        return then(cb, null);
    }


    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D,E> Promise<RT> thenAsync( IQuintetCallback<RT,A,B,C,D,E> cb ) {
        return then(cb, getBG());
    }


    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D,E> Promise<RT> thenOnMain( IQuintetCallback<RT,A,B,C,D,E> cb ) {
        return then(cb, getMain());
    }
}

