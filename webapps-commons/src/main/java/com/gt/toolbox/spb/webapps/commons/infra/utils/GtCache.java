package com.gt.toolbox.spb.webapps.commons.infra.utils;

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
	private GtCacheStoreProvider<K, T> store;
	private Timer cleanupTimer;
	private boolean closed = false;
	private boolean ownCleanupTimer = true;

	/**
	 * Cache simple en memoria
	 * 
	 * @param toLiveSeconds tiempo de vida de cada objeto
	 * @param cleanupDelaySeconds tiempo cada cuánto se ejecuta el cleanup
	 * @param maxItems cantidad máxima de ítems a guardar
	 */
	public GtCache(GtCacheStoreProvider<K, T> store, long toLiveSeconds,
			final long cleanupDelaySeconds, int maxItems) {
		this(store, toLiveSeconds, cleanupDelaySeconds, maxItems, null);
	}

	/**
	 * Cache simple en memoria<br/>
	 * Posibilidad de setear un timer externo para uso de múltiples cache
	 * 
	 * @param toLiveSeconds tiempo de vida de cada objeto
	 * @param cleanupDelaySeconds tiempo cada cuánto se ejecuta el cleanup
	 * @param maxItems cantidad máxima de ítems a guardar
	 * @param cleanupTimer timer que se va a utilizar para programar y ejecutar el cleanup
	 */
	public GtCache(GtCacheStoreProvider<K, T> store, long toLiveSeconds,
			final long cleanupDelaySeconds, int maxItems,
			Timer cleanupTimer) {

		this.store = store;
		this.store.initialize(maxItems);

		if (cleanupTimer == null) {
			ownCleanupTimer = true;
			cleanupTimer = new Timer(true);
		} else {
			ownCleanupTimer = false;
		}
		this.cleanupTimer = cleanupTimer;

		if (toLiveSeconds > 0 && cleanupDelaySeconds > 0) {
			this.cleanupDelay = cleanupDelaySeconds * 1000;
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
		store.put(key, new GtCacheObject<K, T>(value));
	}

	public boolean contains(K key) {
		return store.containsKey(key);
	}

	public T get(K key) {
		return getWrapped(key).value;
	}

	protected GtCacheObject<K, T> getWrapped(K key) {
		return store.get(key).stream()
				.peek(value -> value.lastAccessed = System.currentTimeMillis())
				.findAny()
				.orElse(null);
	}

	public void remove(K key) {
		store.remove(key);
	}

	public int size() {
		return store.size();
	}

	public void cleanup() {

		var deleteKeys = store.getKeysBefore(System.currentTimeMillis() - cleanupDelay);

		store.removeAll(deleteKeys);
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
