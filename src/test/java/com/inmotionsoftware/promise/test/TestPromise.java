package com.inmotionsoftware.promise.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.inmotionsoftware.promise.Promise;
import com.inmotionsoftware.promise.Promise.IDeferred;

public class TestPromise extends PromiseTestCase {

    private class EmptyResolve implements Promise.VoidResolveVoid {
        public void resolve() {}
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

	@Test
	public void testAlways() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}

    @Test
    public void testFail() {
        String name = new Object() {}.getClass().getEnclosingMethod().getName();
        runTests(name);
    }

	@Test
	public void testAsync() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}

	@Test
	public void testPromisePromise() {
		String name = new Object() {}.getClass().getEnclosingMethod().getName();
		runTests(name);
	}

    @AsyncTest(group = "testPromisePromise", fail=true)
    public Promise<Void> testNullPromise() {

        final Throwable[] err = new Throwable[]{null};
        return Promise.resolve("String")
                .thenAsync(new Promise.IResolve<String, String>() {
                    @Override
                    public String resolve(String result) {
                        return result;
                    }
                }).thenOnMain(new Promise.PromiseHandler<Void, String>() {
                    @Override
                    public Promise<Void> resolve(String result) {
                        return null;
                    }

                }).fail(new Promise.IReject() {
                    @Override
                    public void reject(Throwable t) {
                        err[0] = t;
                    }
                }).always(new Promise.IAlways() {
                    public void always() {
                        Assert.assertNotNull(err[0]);
                    }
                });
    }


    @AsyncTest(group = "testAlways")
    public Promise<Void> testAlwaysCalled() {
        final boolean[] suc = new boolean[]{false};
        Promise<Void> p = Promise.resolve().always(new Promise.IAlways() {
            public void always() {
                suc[0] = true;
            }
        });
        Assert.assertTrue(suc[0]);
        return p;
    }

    @AsyncTest(group = "testAlways")
    public Promise<Void> testAlwaysAsync() {
        return Promise.resolve()
                .thenAsync(new EmptyResolve())
                .always(new Promise.IAlways() {
                    public void always() {
                        assertIsBackgroundThread();
                    }
                });
    }

    @AsyncTest(group = "testAlways")
    public Promise<Void> testAlwaysOnMain() {
        return Promise.resolve()
                .thenOnMain(new EmptyResolve())
                .always(new Promise.IAlways() {
                    public void always() {
                        assertIsMainThread();
                    }
                });
    }

    @AsyncTest(group = "testAlways")
    public Promise<Void> testAlwaysOnMain2() {
        return Promise.resolve()
                .thenAsync(new EmptyResolve())
                .alwaysOnMain(new Promise.IAlways() {
                    public void always() {
                        assertIsMainThread();
                    }
                });
    }

    @AsyncTest(group = "testAlways")
    public Promise<Void> testAlwaysOnMain3() {
        return Promise.resolve()
                .alwaysOnMain(new Promise.IAlways() {
                    public void always() {
                        assertIsMainThread();
                    }
                });
    }

    @AsyncTest(group = "testAlways")
    public Promise<Void> testAlwaysAsync2() {
        return Promise.resolve()
                .alwaysAsync(new Promise.IAlways() {
                    public void always() {
                        assertIsBackgroundThread();
                    }
                });
    }

	@AsyncTest(group="testFail", fail=true)
	public Promise<Object> testFailDefaultsToMain() {
		Exception e = new RuntimeException();
		return Promise.reject(e).fail(new Promise.IReject() {
            public void reject(Throwable t) {
                assertIsMainThread();
            }
		});
	}
	
	@AsyncTest(group="testFail", fail=true)
	public Promise<Object> testFailOnMain() {
		Exception e = new RuntimeException();
		return Promise.reject(e).failOnMain(new Promise.IReject() {
            public void reject(Throwable t) {
                assertIsMainThread();
            }
		});
	}

	@AsyncTest(group="testFail", fail=true)
	public Promise<Object> testFailAsync() {
		Exception e = new RuntimeException();
		return Promise.reject(e).failAsync(new Promise.IReject() {
            public void reject(Throwable t) {
                assertIsBackgroundThread();
            }
		});
	}
	
	@AsyncTest(group="testFail", fail=true)
	public Promise<Void> testFailIsCalled() {
        Exception e = new RuntimeException();
        return Promise.reject(e)
        .then(new Promise.VoidResolveVoid() {
            public void resolve() {
                Assert.fail();
            }
        }).fail(new Promise.IReject() {
            public void reject(Throwable t) {
                assertIsMainThread();
                Assert.assertNotNull(t);
            }
        });
	}
	
	@AsyncTest(group="testAsync")
	public Promise<Void> testAsyncThen() {
		Promise<Void> promise = Promise.resolve().thenAsync(new EmptyResolve());

		try { // give it time to complete
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return promise.then(new Promise.VoidResolveVoid() {
            public void resolve() {
                assertIsBackgroundThread();
            }
		});
	}

	
	@AsyncTest(group="testAsync")
	public Promise<String> testMakeAsync() {
		return Promise.makeAsync(new Promise.Deferrable<String>() {

            public void run(IDeferred<String> deferred) throws Exception {
                assertIsBackgroundThread();
                deferred.resolve("done");
            }
		});
	}


	@AsyncTest(group="testAsync")
	public Promise<Integer> testMakeIsOnMain() {
		assertIsMainThread();
		return Promise.make(new Promise.Deferrable<Integer>() {

            public void run(IDeferred<Integer> deferred) throws Exception {
                assertIsMainThread();
                deferred.resolve(1);
            }
		});
	}
	
	@AsyncTest(group="testAsync")
	public Promise<Integer> testMakeOnMain() {
		assertIsMainThread();
		return Promise.makeOnMain(new Promise.Deferrable<Integer>() {
            public void run(IDeferred<Integer> deferred) throws Exception {
                assertIsMainThread();
                deferred.resolve(1);
            }
		});
	}
}
