package minesweeper.solver;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ThreadManager {

	private static ThreadManager manager = null;
	
	private ExecutorService pool;
	
	private ThreadManager(int threads) {
		pool = Executors.newFixedThreadPool(threads);
		//pool = Executors.newCachedThreadPool();
		
	}
	
	public <T> Future<T> submit(Callable<T> task) {
		
		Future<T> future = pool.submit(task);
		
		return future;
	}
	
	
	protected static synchronized void create(int threads) {
		
		if (manager != null) {
			return;
		}
		
		manager = new ThreadManager(threads);
		System.out.println("Created new thread manager");

	}
	
	public static ThreadManager get() {
		return manager;
	}
	
	public static void shutdown() {
		if (manager != null) {
			System.out.println("Requesting thread manager to shut down");
			manager.pool.shutdownNow();
			manager = null;
		}
	}

}
