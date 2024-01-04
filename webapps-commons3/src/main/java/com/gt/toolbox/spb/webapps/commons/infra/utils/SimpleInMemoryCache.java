package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.ArrayList;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LRUMap;

/**
 * 
 * @author PortalTheler
 *
 * @param <K>
 * @param <T>
 */
public class SimpleInMemoryCache<K, T> implements AutoCloseable {

	private long timeToLive;
	private LRUMap<K, SimpleCacheObject> simpleCacheMap;
	CleanUpScheduler cleanUpScheduler;

	protected class SimpleCacheObject {
		public long lastAccessed = System.currentTimeMillis();
		public T value;

		protected SimpleCacheObject(T value) {
			this.value = value;
		}
	}

	/**
	 * Cache simple en memoria
	 * 
	 * @param secondsToLive   tiempo de vida de cada objeto
	 * @param secondsInterval tiempo cada cuánto se fija para ver si debe retirar un
	 *                        objeto
	 * @param maxItems        cantidad máxima de ítems a guardar
	 */
	public SimpleInMemoryCache(long secondsToLive, final long secondsInterval, int maxItems) {
		this.timeToLive = secondsToLive * 1000;

		simpleCacheMap = new LRUMap<>(maxItems);

		if (timeToLive > 0 && secondsInterval > 0) {
			createCleanUpDaemon(secondsInterval);
		}
	}

	public void clear() {
		synchronized (simpleCacheMap) {
			this.simpleCacheMap.clear();
		}
	}

	private void createCleanUpDaemon(final long secondsInterval) {
		this.cleanUpScheduler = new CleanUpScheduler(this, secondsInterval);
		
		this.cleanUpScheduler.start();
	}

	public void put(K key, T value) {
		synchronized (simpleCacheMap) {
			simpleCacheMap.put(key, new SimpleCacheObject(value));
		}
	}

	public T get(K key) {
		synchronized (simpleCacheMap) {
			SimpleCacheObject c = (SimpleCacheObject) simpleCacheMap.get(key);

			if (c == null)
				return null;
			else {
				c.lastAccessed = System.currentTimeMillis();
				return c.value;
			}
		}
	}

	public void remove(K key) {
		synchronized (simpleCacheMap) {
			simpleCacheMap.remove(key);
		}
	}

	public int size() {
		synchronized (simpleCacheMap) {
			return simpleCacheMap.size();
		}
	}

	public void cleanup() {

		long now = System.currentTimeMillis();
		ArrayList<K> deleteKey = null;

		synchronized (simpleCacheMap) {
			MapIterator<K, SimpleCacheObject> itr = simpleCacheMap.mapIterator();

			deleteKey = new ArrayList<K>((simpleCacheMap.size() / 2) + 1);
			K key = null;
			SimpleCacheObject c = null;

			while (itr.hasNext()) {
				key = (K) itr.next();
				c = (SimpleCacheObject) itr.getValue();

				if (c != null && (now > (timeToLive + c.lastAccessed))) {
					deleteKey.add(key);
				}
			}
		}

		for (K key : deleteKey) {
			synchronized (simpleCacheMap) {
				simpleCacheMap.remove(key);
			}

			// Se agrega esto para que le de prioridad a otros procesos
			Thread.yield();
		}
	}

	@Override
	public void close() throws Exception {
		this.cleanUpScheduler.stopCleanUp();
	}

	private class CleanUpScheduler extends Thread {

		long secondsInterval;
		SimpleInMemoryCache<K, T> owner;
		boolean isStopped = false;

		public CleanUpScheduler(SimpleInMemoryCache<K, T> owner, long secondsInterval) {
			super();
			this.owner = owner;
			this.secondsInterval = secondsInterval;
			
			// Al setearlo en true el programa se para igual
			// aunque el thread esté corriendo
			this.setDaemon(true);
		}

		public void run() {
			boolean internalIsStopped = false;
			while (!internalIsStopped) {
				synchronized (this) {
					internalIsStopped = this.isStopped;
				}
				try {
					Thread.sleep(this.secondsInterval * 1000);
				} catch (InterruptedException ex) {
				}
				owner.cleanup();
			}
		}

		public void stopCleanUp() {
			synchronized (this) {
				this.isStopped = true;
			}
		}

	}
}