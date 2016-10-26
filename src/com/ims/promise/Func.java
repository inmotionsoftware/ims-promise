package com.ims.promise;

public interface Func<IN,OUT> {
	public OUT apply(IN in);
}
