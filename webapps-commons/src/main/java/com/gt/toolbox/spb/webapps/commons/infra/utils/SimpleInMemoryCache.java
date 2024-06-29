package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.collections4.map.LRUMap;

/**
 * 
 * @author PortalTheler
 *
 * @param <K>
 * @param <T>
 */
public class SimpleInMemoryCache<K, T> implements Closeable {

	private long cleanupDelay;
	private Map<K, SimpleCacheObject<K, T>> simpleCacheMap;
	Timer cleanupTimer;
	boolean closed = false;
	boolean ownCleanupTimer = true;

	/**
	 * Cache simple en memoria
	 * 
	 * @param toLiveSeconds tiempo de vida de cada objeto
	 * @param cleanupDelaySeconds tiempo cada cuánto se ejecuta el cleanup
	 * @param maxItems cantidad máxima de ítems a guardar
	 */
	public SimpleInMemoryCache(long toLiveSeconds, final long cleanupDelaySeconds, int maxItems) {
		this(toLiveSeconds, cleanupDelaySeconds, maxItems, null);
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
	public SimpleInMemoryCache(long toLiveSeconds, final long cleanupDelaySeconds, int maxItems,
			Timer cleanupTimer) {

		simpleCacheMap = Collections.synchronizedMap(new LRUMap<>(maxItems));

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
		synchronized (simpleCacheMap) {
			this.simpleCacheMap.clear();
		}
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
		synchronized (simpleCacheMap) {
			simpleCacheMap.put(key, new SimpleCacheObject<K, T>(value));
		}
	}

	public boolean contains(K key) {
		return simpleCacheMap.containsKey(key);
	}

	public T get(K key) {
		return getWrapped(key).value;
	}

	protected SimpleCacheObject<K, T> getWrapped(K key) {
		SimpleCacheObject<K, T> c = (SimpleCacheObject<K, T>) simpleCacheMap.get(key);

		if (c == null)
			return null;
		else {
			c.lastAccessed = System.currentTimeMillis();
			return c;
		}
	}

	public void remove(K key) {
		simpleCacheMap.remove(key);
	}

	public int size() {
		return simpleCacheMap.size();
	}

	public void cleanup() {

		long now = System.currentTimeMillis();
		var deleteKey = new ArrayList<K>();

		synchronized (simpleCacheMap) {
			for (var entry : simpleCacheMap.entrySet()) {
				if (entry.getKey() != null
						&& (now > (cleanupDelay + entry.getValue().lastAccessed))) {
					deleteKey.add(entry.getKey());
				}
			}
		}

		synchronized (simpleCacheMap) {
			for (K key : deleteKey) {
				simpleCacheMap.remove(key);
				// Se agrega esto para que le de prioridad a otros procesos
				Thread.yield();
			}
		}
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
