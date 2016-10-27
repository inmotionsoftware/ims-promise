package com.ims.promise.Test;

import com.ims.promise.Promise;
import com.ims.promise.Promise.IPromiseFunc;
import com.ims.tuple.Tuple;

public class Test {
	
	public static void main(String[] args) {
		System.out.println("init"); 
		
		final boolean[] done = new boolean[] {false};
		
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
		.then(new IPromiseFunc<Void,String>() {
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
			public void finish() {
				done[0] = true;
			}
		});
//		.thenCatch( (Throwable t) -> {
//			System.err.print("Error: " + t);
//			done[0] = true;
//		});
		
		System.out.println("promise created!");

		while (!done[0]) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}	
		
		System.out.println("Exiting...");
	}
}
