package com.inmotionsoftware.promise

import java.util.concurrent.Executor

/**
 * Created by bghoward on 8/29/17.
 */

class PromiseFulfilledException: RuntimeException()

open class Promise<OUT> {
    protected sealed class Result<T> {
        class Resolved<T>(val value: T) : Result<T>()
        class Rejected<T>(val error: Throwable) : Result<T>()

        val isRejected get() = this is Rejected<T>
        val isResolved get() = this is Resolved<T>
    }

    protected interface Input<IN> {
        fun resolve(result: Result<IN>)
    }

    private interface Output<OUT> {
        fun add(child: Input<OUT>)

        val isRejected: Boolean get
        val isResolved: Boolean get
        val isFulfilled: Boolean get
    }

    protected abstract class Continuation<IN, OUT>(private val executor: Executor?) : Input<IN>, Output<OUT> {
        private val children: MutableList<Input<OUT>> = mutableListOf()

        override val isRejected: Boolean
            get() = when (this.result) {
                is Result.Resolved -> false
                is Result.Rejected -> true
                else -> false
            }

        override val isResolved: Boolean
            get() = when (this.result) {
                is Result.Resolved -> true
                is Result.Rejected -> false
                else -> false
            }

        override val isFulfilled: Boolean
            get() = when (this.result) {
                is Result.Resolved -> true
                is Result.Rejected -> true
                else -> false
            }

        protected var result: Result<OUT>? = null
            get() = synchronized(this) { return field }
            set(value) {
                if (value == null) throw NullPointerException()
                val list: MutableList<Input<OUT>> = mutableListOf()
                synchronized(this) {
                    // cannot set twice
                    if (field != null) { throw PromiseFulfilledException() }
                    field = value
                    list.addAll(this.children)
                    this.children.clear()
                }
                list.forEach { it.resolve(value) }
            }

        private fun execute(value: IN): Result<OUT> {
            return try {
                val out = dispatch(value)
                Result.Resolved(out)
            } catch (err: Throwable) {
                try {
                    recover(err)
                } catch (err2: Throwable) {
                    Result.Rejected(err2)
                }
            }
        }

        override fun resolve(result: Result<IN>) {
            fun run() {
                when (result) {
                    is Result.Resolved -> this.result = execute(result.value)
                    is Result.Rejected -> this.result = recover(result.error)
                }
                always()
            }

            if (executor != null) {
                executor.run{ run() }
            } else {
                run()
            }
        }

        abstract fun dispatch(value: IN): OUT
        open fun always() {}
        open fun recover(error: Throwable): Result<OUT> = Result.Rejected(error)

        override fun add(child: Input<OUT>) {
            var result = this.result
            synchronized(this) {
                if (this.result != null) {
                   result = this.result
                } else {
                    children.add(child)
                }
            }

            if (result != null) {
                child.resolve(result!!)
            }
        }
    }

    private class BasicContinuation<IN, OUT>(executor: Executor?, val execute: (IN) -> OUT) : Continuation<IN, OUT>(executor) {
        override fun dispatch(value: IN): OUT = execute(value)
    }

    private class RecoverContinuation<T>(executor: Executor?, val execute: (Throwable) -> T) : Continuation<T, T>(executor) {
        override fun dispatch(value: T): T = value

        override fun recover(error: Throwable): Result<T> {
            return try {
                Result.Resolved(execute(error))
            } catch (err: Throwable) {
                Result.Rejected(err)
            }
        }
    }

    private class ErrorContinuation<T>(executor: Executor?, val execute: (Throwable) -> Unit) : Continuation<T, Unit>(executor) {
        override fun dispatch(value: T) {}

        override fun recover(error: Throwable): Result<Unit> {
            execute(error)
            return Result.Rejected(error)
        }
    }

    private class AlwaysContinuation<T>(executor: Executor?, val execute: () -> Unit) : Continuation<T, Unit>(executor) {
        override fun dispatch(value: T) {}

        override fun always() {
            execute()
        }
    }

    private class DeferredContinuation<T>(executor: Executor? = null) : Continuation<T, T>(executor) {
        override fun dispatch(value: T): T = value

        fun resolve(value: T) {
            resolve(Result.Resolved(value))
        }

        fun reject(error: Throwable) {
            resolve(Result.Rejected(error))
        }
    }

    protected class ResolvedContinuation<T>(value: Result<T>) : Continuation<T, T>(null) {
        init {
            this.result = value
        }

        override fun dispatch(value: T): T {
            val result = this.result
            when (result) {
                is Result.Resolved -> return result.value
                is Result.Rejected -> throw result.error
                else -> throw IllegalStateException()
            }
        }
    }

    private var output: Output<OUT>

    val isPending: Boolean get() = !this.isFulfilled
    val isFulfilled: Boolean get() = this.output.isFulfilled
    val isRejected: Boolean get() = this.output.isRejected
    val isResolved: Boolean get() = this.output.isResolved

    constructor(on: Executor? = null, execute: ((OUT) -> Unit, (Throwable) -> Unit) -> Unit) {
        val cont = DeferredContinuation<OUT>()
        this.output = cont

        if (on != null) {
            on.run {
                execute({ cont.resolve(it) }, { cont.reject(it) })
            }
        } else {
            execute({ cont.resolve(it) }, { cont.reject(it) })
        }
    }

    private constructor(output: Output<OUT>) {
        this.output = output
    }

    constructor(value: OUT) {
        this.output = ResolvedContinuation(Result.Resolved(value))
    }

    constructor(error: Throwable) {
        this.output = ResolvedContinuation(Result.Rejected(error))
    }

    fun recover(on: Executor?, execute: (Throwable) -> OUT): Promise<OUT> {
        val cont = RecoverContinuation(on, execute)
        this.output.add(cont)
        return Promise(cont)
    }

    fun <T> then(on: Executor?, execute: (OUT) -> T): Promise<T> {
        val cont = BasicContinuation(on, execute)
        this.output.add(cont)
        return Promise(cont)
    }

    fun catch(on: Executor?, execute: (Throwable) -> Unit): Promise<OUT> {
        val cont = ErrorContinuation<OUT>(on, execute)
        this.output.add(cont)
        return this
    }

    fun always(on: Executor?, execute: () -> Unit ): Promise<OUT> {
        val cont = AlwaysContinuation<OUT>(on, execute)
        this.output.add(cont)
        return this
    }

    fun asVoid(on: Executor?): Promise<Unit> {
        return this.then(on=on) { Unit }
    }

    fun <T> thenp(on: Executor?, execute: (OUT) -> Promise<T>): Promise<T> {
        return Promise { resolve, reject ->
            Unit
            this.then(on = on, execute = execute)
                    .then(on = null) {
                        it.then(on = null) { resolve(it) }.catch(on = null) { reject(it) }
                    }
        }
    }

    fun recoverp(on: Executor?, execute: (Throwable) -> Promise<OUT>): Promise<OUT> {
        return Promise { resolve, reject ->
            Unit
            this.then(on = null) { resolve(it) }
                    .catch(on = on) {
                        execute(it).then(on = null) { resolve(it) }.catch(on = null) { reject(it) }
                    }
        }
    }

    companion object {
        fun <T> deferred(): DeferredPromise<T> {
            return DeferredPromise<T>()
        }

        fun void(): Promise<Unit> {
            return Promise(value=Unit)
        }
    }
}

class DeferredPromise<T> {
    val promise: Promise<T>
    private var _resolve: ((T) -> Unit)? = null
    private var _reject: ((Throwable) -> Unit)? = null

    init {
        promise = Promise<T>{ resolve, reject ->
            this._resolve = resolve
            this._reject = reject
        }
    }

    fun resolve(value: T) {
        this._resolve?.let { it(value) }
    }

    fun reject(error: Throwable) {
        this._reject?.let{ it(error) }
    }
}

fun <T> join(on: Executor?, promises: Collection<Promise<T>>): Promise<Collection<T>> {
    return Promise<Collection<T>>{ resolve, reject ->
        var count = promises.count()
        val results = mutableListOf<T>()
        promises.forEach {
            it.then(on=on) {
                results.add(it)
                if (count-- == 0) {
                    resolve(results)
                }
            }.catch(on=on) { reject(it) }
        }
    }
}
