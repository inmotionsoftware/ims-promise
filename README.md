# IMS Java Promise

A simple to use promise library for Java. The goals of the project are to be easy to use, but powerful enough to be useful.

Features
* Works on Java 7
* Java 8 Lambda friendly
* Android friendly
* Simple, powerful, and adaptable async threading model
* Robust error handling
* Flexible and easy to integrate with other libraries

# How to use IMS-Promise

## Creating a Promise
There are several methods for creating promises, the simplest of which is creating a resolved promise.

### Resolving Promises
A resolved promise is a promise with a value assigned at the time of creation. Any calls to get the value of a resolved promise will dispatch immediately.

```Java
// a resolved Integer promise
Promise<Integer> p1 = Promise.resolve(5);

// resolve a void promise
Promise<Void> p2 = Promise.resolve();
```

### Rejecting Promises
You can also create a rejected promise:

```Java
Exception exc = new RuntimeException("Something bad");
Promise<Integer> p = Promise.reject(exc);
```

### Deferred Promise

More advanced uses may require a promise that can be resolved sometime after the promise has been created. This is accomplished by making a new promise with a Deferrable. A Deferrable will pass an IDeferred object which can be used to later resolve or reject the promise.

```Java
Promise.make( (IDeferred<String> deferred) -> {

    Runnable r = new Runnable() {			
        @Override
        public void run() {
            try {
                String res = doSomething();
                deferred.resolve(res);
            } catch (Exception e) {
                deferred.reject(e);
            }
        }
    };

    new Thread(r).start();		
});
```

## Promise Chaining

Promises can be chained together to create a series of steps to produce a final outcome.

```Java
Promise.resolve(1)
    .then( (Integer i) -> Integer.toString(i) )
    .then( (String s) -> "[" s + "]" );
```

### Promises and Tuples
Tuples can be used to loosely combine or group objects and data together. IMS Promise provides tuples that can be used along with promises. Tuples are automatically unrolled to the next promise in the chain:

```Java
Promise.resolve(1)
    .then( (Integer i) -> Integer.toString(i) )
    .then( (String s) -> "[" s + "]" )
    .then( (String s) -> Tuple.make(1,2.0,s) )
    .then( (Integer i, Double d, String s) -> {
        // do stuff
        return null;
    });
```

## Promise Concurrency

Promises can be dispatched to other threads. This can be controlled by passing an Executor to be exectued on when creating a promise.

```Java
long keepAlive = 10;
int min = 1;
int max = 4;
ThreadPoolExecutor exec = new ThreadPoolExecutor(min, max, keepAlive, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

Promise<String> p =  Promise.make(new MyDeferred(), exec);
```

Or dispatch the next chain on another thread:

```Java
Promise.resolve(1).then(new MyDispatch(), exec);
```

Or better yet, use the default thread pool by calling any of the async methods:

```Java
// Make an async promise
Promise.makeAsync(new MyDeferred());

// Chain async
Promise.resolve().thenAsync( (Void v) -> "string" );
```

Likewise promises can be dispatched on your Main thread by calling any onMain functions. However, IMS Promise is not bound to any particular framework or system, you will have to configure your main executor for your platform this to function as expected.

```Java
// somewhere in your Activity setup...
Promise.setMainExecutor(new Executor() {			
    @Override
    public void execute(Runnable command) {
        MyActivity.this.runOnUiThread(command);
    }
});

// later in your code
Promise.makeAsync(new MyDeferred())
    .thenOnMain( (String s) -> showMessageOnUI(s) );
```

### Error Handling

Errors can be captured by a couple of different ways. Any exceptions thrown in a promise automatically propagate down the chain of promises. Every promise in the chain will have a chance to handle the error.

#### Chaining

You can call the fail method on a chain of promises to capture any exceptions that occur.

```Java
Promise.reject(new RuntimeException())
    .fail( (Throwable t) -> {
        // do something...
    })
```

#### Register a Handler

Alternatively when you add a promise to the chain you can register a handler that will receive notifications for any outcome (including errors).

```Java
Promise.reject(new Handler<String>() {
    @Override
    public Void resolve(Promise<RT> in) {
        return null;
    }

    @Override
    public void reject(Throwable t) {
        // TODO: handle an error
    }

    @Override
    public void always() {}
})
```
