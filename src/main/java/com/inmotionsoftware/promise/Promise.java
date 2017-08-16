package com.inmotionsoftware.promise;

import java.util.ArrayList;
import java.util.Iterator;
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

    public static final String VERSION;

    static {
        VERSION = Promise.class.getPackage().getSpecificationVersion();
    }

    /**
     * @author bghoward
     */
    public interface IReject {
        void reject(Throwable t);
    }

    public interface IRecover<OUT> {
        OUT recover(Throwable t) throws Throwable;
    }

    /**
     * @author bghoward
     */
    public interface IAlways {
        void always();
    }

    /**
     * @author bghoward
     */
    public interface IResolve<OUT, IN> {
        OUT resolve(IN in) throws Exception;
    }

    /**
     * @param <IN>
     * @author bghoward
     */
    public interface VoidResolve<IN> {
        void resolve(IN in) throws Exception;
    }

    /**
     * @author bghoward
     */
    public interface VoidRecover {
        void recover(Throwable t) throws Throwable;
    }

    /**
     * @param <OUT>
     * @author bghoward
     */
    public interface ResolveVoid<OUT> {
        OUT resolve() throws Exception;
    }

    /**
     * @author bghoward
     */
    public interface VoidResolveVoid {
        void resolve() throws Exception;
    }

    /**
     * @author bghoward
     */
    public interface IPromiseResolve<OUT, IN> extends IResolve<Promise<OUT>, IN> {
        @Override
        Promise<OUT> resolve(IN in) throws Exception;
    }

    /**
     * @param <OUT>
     * @param <IN>
     * @author bghoward
     */
    public static abstract class Handler<OUT, IN> implements IResolve<OUT, IN>, IReject, IAlways, IRecover<OUT> {
        @Override
        public abstract OUT resolve(IN in) throws Exception;

        @Override
        public void reject(Throwable t) {
        }

        @Override
        public OUT recover(Throwable t) throws Throwable {
            throw t;
        }

        @Override
        public void always() {
        }
    }

    /**
     * @param <OUT>
     * @param <IN>
     */
    private static class ProxyHandler<OUT, IN> extends Handler<OUT, IN> {

        private IResolve<OUT, IN> mResolve;
        private IReject mReject;
        private IRecover<OUT> mRecover;
        private IAlways mAlways;

        ProxyHandler(IResolve<OUT, IN> resolve, IReject reject, IAlways always) {
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
        public OUT recover(Throwable t) throws Throwable {
            if (mRecover != null) return mRecover.recover(t);
            throw t;
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
     * @param <OUT>
     * @param <IN>
     * @author bghoward
     */
    public static abstract class PromiseHandler<OUT, IN> extends Handler<Promise<OUT>, IN> implements IPromiseResolve<OUT, IN> {
        @Override
        public abstract Promise<OUT> resolve(IN in) throws Exception;
    }

    /**
     * @param <OUT>
     * @param <A>
     * @author bghoward
     */
    public interface IUnaryCallback<OUT, A> {
        OUT resolve(A a) throws Exception;
    }

    /**
     * @param <OUT>
     * @param <A>
     * @param <B>
     * @author bghoward
     */
    public interface IPairCallback<OUT, A, B> {
        OUT resolve(A a, B b) throws Exception;
    }

    /**
     * @param <OUT>
     * @param <A>
     * @param <B>
     * @param <C>
     * @author bghoward
     */
    public interface ITripletCallback<OUT, A, B, C> {
        OUT resolve(A a, B b, C c) throws Exception;
    }

    /**
     * @param <OUT>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @author bghoward
     */
    public interface IQuartetCallback<OUT, A, B, C, D> {
        OUT resolve(A a, B b, C c, D d) throws Exception;
    }

    /**
     * @param <OUT>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <E>
     * @author bghoward
     */
    public interface IQuintetCallback<OUT, A, B, C, D, E> {
        OUT resolve(A a, B b, C c, D d, E e) throws Exception;
    }

    /**
     * @param <IN>
     * @author bghoward
     */
    public interface IDeferred<IN> {
        void resolve(IN in);

        void reject(Throwable e);
    }

    /**
     * @param <IN>
     * @author bghoward
     */
    public interface Deferrable<IN> {
        void run(IDeferred<IN> resolve) throws Exception;
    }

    /**
     * @param <T>
     */
    public static class DeferredPromise<T> extends Promise<T> {
        final DeferredContinuation2<T> mCont;

        DeferredPromise(Executor exe) {
            super(new DeferredContinuation2<T>(exe));
            mCont = (DeferredContinuation2<T>) mOut;
        }

        public void resolvePromise(T in) {
            mCont.resolve(in);
        }

        public void rejectPromise(Throwable e) {
            mCont.reject(e);
        }
    }


    /**
     * @param <OUT>
     * @author bghoward
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
     * @param <T> Promises of type T
     */
    public final static class AggregateResults<T> {
        private final List<T> mSucceeded = new ArrayList<>();
        private final List<Throwable> mFailed = new ArrayList<>();

        private AggregateResults() {
        }

        void success(T suc) {
            synchronized (mSucceeded) {
                mSucceeded.add(suc);
            }
        }

        synchronized void failed(Throwable t) {
            synchronized (mFailed) {
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
     * @param <IN>
     * @author bghoward
     */
    private interface IInComponent<IN> {
        void resolve(Result<IN> result);
    }

    /**
     * @param <OUT>
     * @author bghoward
     */
    private interface IOutComponent<OUT> {
        void addChild(IInComponent<OUT> child);

        Executor getExecutor();
    }

    /**
     * @param <OUT>
     * @param <IN>
     * @author bghoward
     */
    private static abstract class BaseContinuation<OUT, IN> implements IOutComponent<OUT>, IInComponent<IN> {

        private List<IInComponent<OUT>> mChildren;
        private Result<OUT> mResult;
        private final Executor mExecutor;

        BaseContinuation(Executor exe) {
            mExecutor = exe;
        }

        @Override
        public void resolve(final Result<IN> result) {
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

            List<IInComponent<OUT>> children = null;
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
     * @param <T>
     * @author bghoward
     */
    private static class ResolvedContinuation<T> extends BaseContinuation<T, T> {
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
     * @param <T>
     */
    private static class DeferredContinuation2<T> extends BaseContinuation<T, T> {
        private boolean mResolved = false;

        DeferredContinuation2(Executor exe) {
            super(exe);
        }

        @Override
        protected void resolve(T in) {
            assert (!mResolved);
            mResolved = true;
            dispatchResult(new Result<T>(in, null));
        }

        @Override
        protected void reject(Throwable t) {
            assert (!mResolved);
            mResolved = true;
            dispatchResult(new Result<T>(null, t));
        }
    }

    /**
     * @param <T>
     * @author bghoward
     */
    private static class DeferredContinuation<T> extends DeferredContinuation2<T> {

        private final Deferrable<T> mDeferrable;

        DeferredContinuation(Deferrable<T> d, Executor exe) {
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
    }

    /**
     * @param <OUT>
     * @param <IN>
     * @author bghoward
     */
    private static class CallbackContinuation<OUT, IN> extends BaseContinuation<OUT, IN> {

        private final Handler<OUT, IN> mCallback;

        CallbackContinuation(Handler<OUT, IN> cb, Executor exe) {
            super(exe);
            if (cb == null)
                throw new NullPointerException("CallbackContinuation cannot have a null callback");
            mCallback = cb;
        }

        @Override
        protected void resolve(IN in) {
            try {
                Result<OUT> result;
                try {
                    OUT out = mCallback.resolve(in);
                    result = new Result<>(out, null);
                } catch (Throwable t) {
                    try {
                        OUT out = mCallback.recover(t);
                        result = new Result<>(out, null);
                    } catch (Throwable t2) {
                        result = new Result<>(null, t2);
                    }
                }
                dispatchResult(result);
            } finally {
                mCallback.always();
            }
        }

        @Override
        protected void reject(Throwable t) {
            try {
                Result<OUT> result;
                try {
                    OUT out = mCallback.recover(t);
                    result = new Result<>(out, null);
                } catch (Throwable t2) {
                    try {
                        mCallback.reject(t2);
                    } catch (Throwable e) {
                        // ignore: we don't want the handler to cause our promise chain to stop
                    }
                    // forward the error
                    result = new Result<>(null, t2);
                }
                dispatchResult(result);

            } finally {
                mCallback.always(); // we should always notify
            }
        }
    }

    private static Executor gMain;
    private static Executor gBack;
    protected final IOutComponent<OUT> mOut;

    /**
     * @return the register executor for the main thread
     */
    public static synchronized Executor getMainExecutor() {
        return gMain;
    }

    /**
     * @return the register executor for background tasks
     */
    public static synchronized Executor getBackgroundExecutor() {


        synchronized (Promise.class) {
            if (gBack == null) {
                int cores = Runtime.getRuntime().availableProcessors();
                BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
                ThreadPoolExecutor exec = new ThreadPoolExecutor(2, cores, 10, TimeUnit.MINUTES, queue);
                setBackgroundExecutor(exec); // set the default
            }
            return gBack;
        }
    }

    /**
     * @param main the executor for the main thread
     */
    public static synchronized void setMainExecutor(Executor main) {
        gMain = main;
    }

    /**
     * @param bg the executor for background threads
     */
    public static synchronized void setBackgroundExecutor(Executor bg) {
        gBack = bg;
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
    public static <T> Promise<AggregateResults<T>> all(final Iterable<Promise<T>> iter) {
        final DeferredPromise<AggregateResults<T>> rt = Promise.make();

        final AggregateResults<T> results = new AggregateResults<>();

        int total = 0;
        for (Iterator<Promise<T>> it = iter.iterator(); it.hasNext(); it.next()) {
            ++total;
        }

        final AtomicInteger count = new AtomicInteger(total);

        for (Promise<T> promise : iter) {
            promise.then(new Handler<Void, T>() {
                @Override
                public Void resolve(T t) throws Exception {
                    results.mSucceeded.add(t);
                    return (Void) null;
                }

                public void reject(Throwable t) {
                    results.mFailed.add(t);
                }

                public void always() {
                    // done
                    if (count.decrementAndGet() == 0) {
                        rt.resolvePromise(results);
                    }
                }
            });
        }

        return rt;
    }

    /**
     * @param in the item to resolve this promise with
     * @return a resolved promise
     */
    public static <IN> Promise<IN> resolve(IN in) {
        return new Promise<>(new ResolvedContinuation<IN>(in, null));
    }

    /**
     * @return a resolved promise of type Void
     */
    public static Promise<Void> resolve() {
        return resolve((Void) null);
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
    public static <OUT> Promise<OUT> makeAsync(Deferrable<OUT> def) {
        return make(def, getBackgroundExecutor());
    }

    /**
     * @param def the deferrable this promise will be resolved with
     * @return a new Promise to be resolved by the Deferrable
     */
    public static <OUT> Promise<OUT> makeOnMain(Deferrable<OUT> def) {
        return make(def, getMainExecutor());
    }

    public static <OUT> DeferredPromise<OUT> make() {
        return new DeferredPromise<>(null);
    }

    public static <OUT> DeferredPromise<OUT> make(Executor exe) {
        return new DeferredPromise<>(exe);
    }


    /**
     * @param def the deferrable this promise will be resolved with
     * @return a new Promise to be resolved by the Deferrable
     */
    public static <OUT> Promise<OUT> make(Deferrable<OUT> def, Executor exe) {
        DeferredContinuation<OUT> cont = new DeferredContinuation<>(def, exe);
        cont.start();
        return new Promise<>(cont);
    }

    /**
     * @param def the deferrable this promise will be resolved with
     * @return a new Promise to be resolved by the Deferrable
     */
    public static <OUT> Promise<OUT> make(Deferrable<OUT> def) {
        return make(def, null);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final PromiseHandler<RT, OUT> handler) {
        return then(handler, (Executor) null);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final IResolve<RT, OUT> handler, Executor exe) {
        return this.then(new ProxyHandler<>(handler, null, null), exe);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public Promise<Void> then(final VoidResolve<OUT> handler) {
        return this.then(handler, (Executor) null);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public Promise<Void> then(final VoidResolve<OUT> handler, Executor exe) {
        return this.then(new Handler<Void, OUT>() {
            @Override
            public Void resolve(OUT in) throws Exception {
                handler.resolve(in);
                return null;
            }
        }, exe);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public Promise<Void> then(final VoidResolveVoid handler) {
        return then(handler, (Executor) null);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public Promise<Void> then(final VoidResolveVoid handler, Executor exe) {
        return this.then(new Handler<Void, OUT>() {
            @Override
            public Void resolve(OUT in) throws Exception {
                handler.resolve();
                return null;
            }
        }, exe);
    }


    /**
     * @param handler the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final ResolveVoid<RT> handler) {
        return this.then(handler, (Executor) null);
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final ResolveVoid<RT> handler, Executor exe) {
        return this.then(new Handler<RT, OUT>() {
            @Override
            public RT resolve(OUT in) throws Exception {
                return handler.resolve();
            }
        }, exe);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public Promise<Void> then(final VoidResolve<OUT> resolve, IReject reject, IAlways always, Executor exe) {
        ProxyHandler<Void, OUT> proxy = new ProxyHandler<>(new IResolve<Void, OUT>() {
            @Override
            public Void resolve(OUT out) throws Exception {
                resolve.resolve(out);
                return null;
            }
        }, reject, always);

        return then(proxy, exe);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final ResolveVoid<RT> resolve, IReject reject, IAlways always, Executor exe) {
        return then(new ProxyHandler<>(new IResolve<RT, OUT>() {
            @Override
            public RT resolve(OUT out) throws Exception {
                return resolve.resolve();
            }
        }, reject, always), exe);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public Promise<Void> then(final VoidResolveVoid resolve, IReject reject, IAlways always, Executor exe) {
        return then(new ProxyHandler<>(new IResolve<Void, OUT>() {
            @Override
            public Void resolve(OUT out) throws Exception {
                resolve.resolve();
                return null;
            }
        }, reject, always), exe);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> then(IResolve<RT, OUT> resolve, IReject reject, IAlways always, Executor exe) {
        return then(new ProxyHandler<>(resolve, reject, always), exe);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> then(IResolve<RT, OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, (Executor) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> then(VoidResolve<OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, (Executor) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> then(ResolveVoid<RT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, (Executor) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> then(VoidResolveVoid resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, (Executor) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> then(IResolve<RT, OUT> resolve, IReject reject) {
        return then(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> then(VoidResolve<OUT> resolve, IReject reject) {
        return then(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(ResolveVoid<RT> resolve, IReject reject) {
        return then(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> then(VoidResolveVoid resolve, IReject reject) {
        return then(resolve, reject, (IAlways) null);
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final Handler<RT, OUT> cb) {
        return this.then(cb, (Executor) null);
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final IResolve<RT, OUT> cb) {
        return this.then(cb, (Executor) null);
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final IPromiseResolve<RT, OUT> cb) {
        return this.then(cb, (Executor) null);
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(final IPromiseResolve<RT, OUT> cb, Executor exe) {
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
    public <RT> Promise<RT> then(final PromiseHandler<RT, OUT> handler, final Executor exe) {
        // This is a special case of a promise returning another promise

        final DeferredPromise<RT> rt = Promise.make();

        // call the outer promise and wait for the promised result
        then((Handler<Promise<RT>, OUT>) handler, exe).then(new Handler<Void, Promise<RT>>() {

            @Override
            public Void resolve(Promise<RT> in) {

                if (in == null) {
                    rt.rejectPromise(new NullPointerException());
                    return null;
                }

                // now we call the inner promise and forward the results
                in.then(new Handler<Void, RT>() {
                    @Override
                    public Void resolve(RT in) {
                        rt.resolvePromise(in);
                        return null;
                    }

                    @Override
                    public void reject(Throwable t) {
                        rt.rejectPromise(t);
                    }

                    @Override
                    public void always() {
                    }
                }, exe);
                return null;
            }

            @Override
            public Void recover(Throwable t) throws Throwable {
                throw t;
            }

            @Override
            public void reject(Throwable t) {
                rt.rejectPromise(t);
            }

            @Override
            public void always() {
            }
        });

        return rt;
    }

    /**
     * @param handler the handler for the next promise in the chain
     * @param exe     the executor that will process this promise
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> then(Handler<RT, OUT> handler, Executor exe) {
        if (exe == null) { // inherit from the parent
            exe = mOut.getExecutor();
        }

        CallbackContinuation<RT, OUT> child = new CallbackContinuation<>(handler, exe);
        mOut.addChild(child);
        return new Promise<>(child);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenAsync(IResolve<RT, OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getBackgroundExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> thenAsync(VoidResolve<OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getBackgroundExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenAsync(ResolveVoid<RT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getBackgroundExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> thenAsync(VoidResolveVoid resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getBackgroundExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenAsync(IResolve<RT, OUT> resolve, IReject reject) {
        return thenAsync(resolve, reject, null);
    }


    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> thenAsync(VoidResolve<OUT> resolve, IReject reject) {
        return thenAsync(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenAsync(ResolveVoid<RT> resolve, IReject reject) {
        return thenAsync(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> thenAsync(VoidResolveVoid resolve, IReject reject) {
        return thenAsync(resolve, reject, (IAlways) null);
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenAsync(IPromiseResolve<RT, OUT> cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenAsync(IResolve<RT, OUT> cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public Promise<Void> thenAsync(VoidResolve<OUT> cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenAsync(ResolveVoid<RT> cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public Promise<Void> thenAsync(VoidResolveVoid cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenAsync(Handler<RT, OUT> cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenAsync(PromiseHandler<RT, OUT> cb) {
        return then(cb, getBackgroundExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public Promise<Void> thenOnMain(VoidResolve<OUT> cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenOnMain(ResolveVoid<RT> cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenOnMain(IResolve<RT, OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getMainExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> thenOnMain(VoidResolve<OUT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getMainExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenOnMain(ResolveVoid<RT> resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getMainExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @param always  notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<Void> thenOnMain(VoidResolveVoid resolve, IReject reject, IAlways always) {
        return then(resolve, reject, always, getMainExecutor());
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenOnMain(IResolve<RT, OUT> resolve, IReject reject) {
        return thenOnMain(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> thenOnMain(VoidResolve<OUT> resolve, IReject reject) {
        return thenOnMain(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public <RT, IN> Promise<RT> thenOnMain(ResolveVoid<RT> resolve, IReject reject) {
        return thenOnMain(resolve, reject, (IAlways) null);
    }

    /**
     * @param resolve notified if the promise is successfully resolved
     * @param reject  notified if the promise is rejected
     * @return a new promise chained by this one
     */
    public Promise<Void> thenOnMain(VoidResolveVoid resolve, IReject reject) {
        return thenOnMain(resolve, reject, (IAlways) null);
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public Promise<Void> thenOnMain(VoidResolveVoid cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenOnMain(PromiseHandler<RT, OUT> cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenOnMain(IPromiseResolve<RT, OUT> cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenOnMain(IResolve<RT, OUT> cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT> Promise<RT> thenOnMain(Handler<RT, OUT> cb) {
        return then(cb, getMainExecutor());
    }

    /**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<OUT> alwaysAsync(IAlways always) {
        return this.always(always, getBackgroundExecutor());
    }

    /**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<OUT> alwaysOnMain(IAlways always) {
        return this.always(always, getMainExecutor());
    }

    /**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<OUT> always(IAlways always) {
        return this.always(always, null);
    }

    /**
     * @param always notified regardless of the outcome
     * @return a new promise chained by this one
     */
    public Promise<OUT> always(IAlways always, Executor exe) {
        return this.then((IResolve<OUT, OUT>) null, null, always, exe);
    }

    public Promise<Void> recoverOnMain(final VoidRecover recover) {
        return this.recover(recover, getMainExecutor());
    }

    public Promise<OUT> recoverOnMain(final IRecover<OUT> recover) {
        return this.recover(recover, getMainExecutor());
    }

    public Promise<Void> recoverAsync(final VoidRecover recover) {
        return this.recover(recover, getBackgroundExecutor());
    }

    public Promise<Void> recover(final VoidRecover recover) {
        return this.recover(recover, null);
    }

    public Promise<OUT> recoverAsync(final IRecover<OUT> recover) {
        return this.recover(recover, getBackgroundExecutor());
    }

    public Promise<Void> recover(final VoidRecover recover, final Executor exe) {
        return this.then(new Handler<Void, OUT>() {
            @Override
            public Void resolve(OUT out) throws Exception {
                return null;
            }

            @Override
            public Void recover(Throwable t) throws Throwable {
                recover.recover(t);
                return null;
            }
        }, exe);
    }

    public Promise<OUT> recover(final IRecover<OUT> recover) {
        return this.recover(recover, (Executor)null);
    }


	public Promise<OUT> recover(final IRecover<OUT> recover, final Executor exe) {
		return this.then(new Handler<OUT, OUT>() {
            @Override
            public OUT resolve(OUT out) throws Exception {
                return out;
            }

            @Override
            public OUT recover(Throwable t) throws Throwable {
                return recover.recover(t);
            }
        }, exe);
	}

	/**
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
	 */
	public Promise<OUT> failOnMain(final IReject reject) {
		return this.fail(reject, getMainExecutor());
	}
	
	/**
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
	 */
	public Promise<OUT> fail(final IReject reject) {
		return this.fail(reject, null);
	}
	
	/**
     * @param reject notified if the promise is rejected
     * @return a new promise chained by this one
	 */
	public Promise<OUT> failAsync(final IReject reject) {
		return this.fail(reject, getBackgroundExecutor());
	}
	
	/**
     * @param reject notified if the promise is rejected
     * @param exe the executor that will process this promise
     * @return a new promise chained by this one
	 */
	public Promise<OUT> fail(final IReject reject, final Executor exe) {

		this.then(new Handler<OUT, OUT>() {
			@Override
			public OUT resolve(OUT out) throws Exception {
				return out;
			}

			@Override
			public void reject(Throwable t) {
				reject.reject(t);
			}
		});

		return this;
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
        return this.then(cb, getBackgroundExecutor());
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B> Promise<RT> thenOnMain( IPairCallback<RT,A,B> cb ) {
        return this.then(cb, getMainExecutor());
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
        return then(cb, getBackgroundExecutor());
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C> Promise<RT> thenOnMain( ITripletCallback<RT,A,B,C> cb) {
        return then(cb, getMainExecutor());
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
        return then(cb, getBackgroundExecutor());
    }

    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D> Promise<RT> thenOnMain( IQuartetCallback<RT,A,B,C,D> cb) {
        return then(cb, getMainExecutor());
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
        return then(cb, getBackgroundExecutor());
    }


    /**
     *
     * @param cb the handler for the next promise in the chain
     * @return a new promise chained by this one
     */
    public <RT,A,B,C,D,E> Promise<RT> thenOnMain( IQuintetCallback<RT,A,B,C,D,E> cb ) {
        return then(cb, getMainExecutor());
    }
}

