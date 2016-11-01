package com.ims.promise.Test;

import com.ims.promise.Promise;
import com.ims.promise.Promise.IDeferred;
import com.ims.promise.Promise.IResolve;
import com.ims.promise.Promise.PromiseHandler;
import com.ims.tuple.Tuple;

public class Test {
	
	public static void main(String[] args) {
		System.out.println("init"); 
		
		final boolean[] done = new boolean[] {false};	

		Promise.resolve(1)
		.then( (Integer i) -> Integer.toString(i) )
		.then( (String s) -> s + "" )
		.then( (String s) -> Tuple.make(1,2,s) )
		.then( (Integer a, Integer b, String c) -> "" )
		.thenAsync( (String s) -> s.split(".") )
		.thenOnMain( (String[] s) -> Integer.parseInt(s[0]) )
		.fail( (Throwable t) -> {
			System.out.println("Error: " + t);
		});
		
		Promise.resolve( (Void) -> 5 )
		.then( (Integer i) -> {
			if (i > 0) throw new RuntimeException("ouch!");
			return i+3;
		})
		.then( (Integer i) -> Tuple.make(i,2) )
		.then( (Integer a, Integer b) -> Integer.toString(a*b) )
		.then( (String s) -> Promise.async(s, (String v) -> {
			Thread.sleep(2000);
			return ":) " + v;
		}))
		.then( (String s ) -> s + " :(")
		.then(new PromiseHandler<Void,String>() {
			@Override
			public Promise<Void> resolve(String in) throws Exception {
				System.out.println("finished!");
				return null;
			}
			
			@Override
			public void reject(Throwable t) {
				System.err.print("Error: " + t);
			}
			
			@Override
			public void always() {
				done[0] = true;
			}
		});
		
		String strnum = "...";
		Promise.make((IDeferred<Integer> d) -> {
			try {
				Integer i = Integer.parseInt(strnum);
				d.resolve(i);
			} catch (NumberFormatException e) {
				d.reject(e);
			}
		});

		System.out.println("promise created!");

		while (!done[0]) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}	
		
		System.out.println("Exiting...");
	}
}
