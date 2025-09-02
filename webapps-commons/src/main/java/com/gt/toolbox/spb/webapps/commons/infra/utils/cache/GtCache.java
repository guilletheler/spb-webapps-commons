package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import java.io.Closeable;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author PortalTheler
 *
 * @param <K>
 * @param <T>
 */
public class GtCache<K, T> implements Closeable {

	private long cleanupDelay;
	private long timeToLive;
	private GtCacheStoreProvider<K, T> store;
	private Timer cleanupTimer;
	private boolean closed = false;
	private boolean ownCleanupTimer = true;

	/**
	 * Cache simple en memoria
	 * 
	 * @param toLiveSeconds tiempo de vida de cada objeto
	 * @param cleanupDelaySeconds tiempo cada cuánto se ejecuta el cleanup
	 */
	public GtCache(GtCacheStoreProvider<K, T> store, long toLiveSeconds,
			final long cleanupDelaySeconds) {
		this(store, toLiveSeconds, cleanupDelaySeconds, null);
	}

	/**
	 * Cache simple en memoria<br/>
	 * Posibilidad de setear un timer externo para uso de múltiples cache
	 * 
	 * @param toLiveSeconds tiempo de vida de cada objeto
	 * @param cleanupDelaySeconds tiempo cada cuánto se ejecuta el cleanup
	 * @param cleanupTimer timer que se va a utilizar para programar y ejecutar el cleanup, en caso
	 *        de ser nulo se creará uno nuevo
	 */
	public GtCache(GtCacheStoreProvider<K, T> store, long timeToLive,
			final long cleanupDelay,
			Timer cleanupTimer) {

		this.store = store;
		this.timeToLive = timeToLive;

		if (cleanupTimer == null) {
			ownCleanupTimer = true;
			cleanupTimer = new Timer(true);
		} else {
			ownCleanupTimer = false;
		}
		this.cleanupTimer = cleanupTimer;

		if (timeToLive > 0 && cleanupDelay > 0) {
			this.cleanupDelay = cleanupDelay;
			scheduleCleanup();
		}

	}

	public void clear() {
		store.clear();
	}

	private void scheduleCleanup() {

		if (this.cleanupTimer != null) {
			this.cleanupTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					cleanup();
					if (!closed) {
						scheduleCleanup();
					}
				}
			}, this.cleanupDelay);
		}
	}

	public void put(K key, T value) {
		store.put(key, value);
	}

	public boolean contains(K key) {
		return store.containsKey(key);
	}

	public T get(K key) {
		return store.get(key).orElse(null);
	}

	public void remove(K key) {
		store.remove(key);
	}

	public int size() {
		return store.size();
	}

	public void cleanup() {
		store.cleanUp(System.currentTimeMillis() - timeToLive);
	}

	@Override
	public void close() throws IOException {
		this.closed = true;
		if (ownCleanupTimer && cleanupTimer != null) {
			cleanupTimer.cancel();
			cleanupTimer = null;
		}
	}

}
