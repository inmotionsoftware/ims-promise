# IMS Java Promise

A simple to use promise library for Java. The goals of the project are to be easy to use, but powerful enough to be useful.

Features
* Java 7 support
* Android support
* Java 8 Lambda friendly
* Simple pluggable system for background async dispatching using Executor


```Java
String strnum = "...";
Promise p = Promise.make((IDeferred<Integer> d) -> {
    try {
        Integer i = Integer.parseInt(strnum);
        d.resolve(i);
    } catch (NumberFormatException e) {
        d.reject(e);
    }
});
```
