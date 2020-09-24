package com.sparklicorn.util.concurrency;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains a collection of worker threads that continuously poll a work queue
 * for runnable tasks. A ThreadPool is marked as active upon creation, and inactive
 * after calling {@link ThreadPool#shutdown()}, at which point the pool will no longer
 * accept further work items.
 * <br><br>
 * Once in the shutdown phase, a ThreadPool cannot become active
 * again. Work items left in the queue will continue to be processed, allowing
 * graceful shutdown, or in the case of {@link ThreadPool#shutdown(bool)} if false is passed,
 * all work items will be purged from the queue and all threads interrupted.
 * A callback function can be added to the graceful shutdown that will be invoked only
 * after all worker threads have stopped.
 * <br><br>
 * ThreadPool contains static helper methods for performing predetermined batches of work,
 * or performing a repeated task a prescribed number of times.
 */
public class ThreadPool {

	private Vector<Thread> threads;
	private LinkedBlockingQueue<Runnable> workQueue;
	private AtomicBoolean active;

	/**
	 * Creates a new ThreadPool. The pool is empty - it contains no worker threads
	 * until added via {@link ThreadPool#addThreads(int)}.
	 */
	public ThreadPool() {
		this(0);
	}

	/**
	 * Creates a new ThreadPool with the specified number of threads. The pool is
	 * immediately active, meaning worker threads are actively polling the work
	 * queue for tasks.1
	 *
	 * @param numThreads - Number of threads to create.
	 */
	public ThreadPool(int numThreads) {
		this.workQueue = new LinkedBlockingQueue<>();
		this.active = new AtomicBoolean(true);
		this.threads = new Vector<>();
		addThreads(numThreads);
	}

	/**
	 * Closes the work queue from further task items. Active threads are allowed to
	 * continue processing work left in the queue to shutdown gracefully. Once the
	 * pool has been shut down, it cannot be restarted. To shutdown the ThreadPool
	 * immediately, see {@link ThreadPool#shutdown(boolean)}.
	 */
	public void shutdown() {
		shutdown(false);
	}

	/**
	 * Closes the work queue from further task items. Active threads are allowed to
	 * continue processing work left in the queue to shutdown gracefully. After the
	 * queue has been depleted and all threads stop, the given callback function is
	 * invoked. Once the pool has been shut down, it cannot be restarted.
	 *
	 * @param callback - A function to invoked after all tasks have completed.
	 * @return A standalone Thread that is watching for the ThreadPool to complete
	 * shutdown and run the callback function.
	 */
	public Thread shutdown(Runnable callback) {
		shutdown();

		Thread poolWatcher = new Thread(() -> {
			boolean keepWatching = true;
			while (keepWatching) {
				for (Thread t : threads) {
					if (t.isAlive()) {
						try {
							Thread.sleep(100L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
				}
				keepWatching = false;
			}

			if (callback != null) {
				callback.run();
			}
		});

		poolWatcher.start();
		return poolWatcher;
	}

	/**
	 * Closes the work queue from further task items.
	 * If force is true, then this immediately shuts down the pool by interrupting
	 * all worker threads and clearing out the work queue.
	 */
	public void shutdown(boolean force) {
		active.set(false);
		if (force) {
			workQueue.clear();
			for (Thread t : threads) {
				t.interrupt();
			}
		}
	}

	/**
	 * Adds and starts the specified number of threads. Threads will
	 * immediately begin polling for work items. If the pool is shutdown,
	 * then this does nothing.
	 * @param numThreads - Number of threads to create.
	 */
	public void addThreads(int numThreads) {
		if (active.get()) {
			for (int n = 0; n < numThreads; n++) {
				startThread();
			}
		}
	}

	// Creates and starts a new worker thread.
	private void startThread() {
		Thread newThread = new Thread(() -> {
			while (active.get() || workQueue.size() > 0) {
				try {
					Runnable workItem = workQueue.poll(1L, TimeUnit.SECONDS);
					if (workItem != null) {
						workItem.run();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		threads.addElement(newThread);
		newThread.start();
	}

	/**
	 * Enqueues a task for processing by the ThreadPool and immediatey returns
	 * a Future object from which the result of the task can be retrieved later.
	 * If the ThreadPool is shutdown, or no threads are active, this returns null.
	 * @param <T> Return type of the given task function.
	 * @param task - A Callable to be invoked later.
	 * @return A Future object to retrieve the return result of task after processing.
	 */
	public synchronized <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> result = null;

		if (active.get()) {
			result = new FutureTask<>(task);
			workQueue.offer(result);
		}

		return result;
	}

	/**
	 * Enqueues a task for processing by the ThreadPool.
	 * @param task - A Runnable to be invoked later.
	 */
	public synchronized void submit(Runnable task) {
		if (active.get()) {
			workQueue.offer(task);
		}
	}

	/**
	 * Creates a new ThreadPool with the specified number of threads, and enqueues
	 * the given work items. The ThreadPool is shutdown immediately so no other work
	 * items may be added. After all work has processed, the callback function is invoked.
	 * @param workload - Tasks for the ThreadPool to process.
	 * @param numThreads - Number of threads to create in the pool.
	 * @param callback - A function to invoke after all tasks have completed.
	 * @return ThreadPool object performing the work batch.
	 */
	public static ThreadPool doBatch(List<Runnable> workload, int numThreads, Runnable callback) {
		ThreadPool pool = new ThreadPool(numThreads);
		for (Runnable r : workload) {
			pool.submit(r);
		}
		pool.shutdown(callback);
		return pool;
	}

	/**
	 * Creates a new ThreadPool with the specified number of threads, and enqueues
	 * the given work items. The ThreadPool is shutdown immediately so no other work
	 * items may be added.
	 * @param workload - Tasks for the ThreadPool to process.
	 * @param numThreads - Number of threads to create in the pool.
	 * @return ThreadPool object performing the work batch.
	 */
	public static ThreadPool doBatch(List<Runnable> workload, int numThreads) {
		return doBatch(workload, numThreads, null);
	}

	/**
	 * Creates a new ThreadPool with the specified number of threads, and equeues task the
	 * given number of times. The pool is shutdown immediately so no other work
	 * items may be added. A callback function is invoked after task has finished processing
	 * the specified number of times.
	 * @param task - Task for the ThreadPool to process.
	 * @param numTimes - Number of times task should be executed.
	 * @param numThreads - Number of threads to create in the pool.
	 * @param callback - A function to invoke after the pool completes processing.
	 * @return ThreadPool object performing the task.
	 */
	public static ThreadPool repeatTask(Runnable task, int numTimes, int numThreads, Runnable callback) {
		if (numTimes < 0) {
			throw new IllegalArgumentException("numTimes must be nonnegative");
		}

		ThreadPool pool = new ThreadPool(numThreads);
		for (int i = 0; i < numTimes; i++) {
			pool.submit(task);
		}
		pool.shutdown(callback);
		return pool;
	}

	/**
	 * Creates a new ThreadPool with the specified number of threads, and equeues task the
	 * given number of times. The pool is shutdown immediately so no other work
	 * items may be added.
	 * @param task - Task for the ThreadPool to process.
	 * @param numTimes - Number of times task should be executed.
	 * @param numThreads - Number of threads to create in the pool.
	 * @return ThreadPool object performing the task.
	 */
	public static ThreadPool repeatTask(Runnable task, int numTimes, int numThreads) {
		return repeatTask(task, numTimes, numThreads, null);
	}
}
