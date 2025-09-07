package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.collections4.map.LRUMap;
import org.springframework.scheduling.annotation.Async;

public class InMemoryCacheStoreProvider<K, T> implements GtCacheStoreProvider<K, T> {

    private final Map<K, GtCacheObject<T>> simpleCacheMap;
    private final Queue<K> keysToDelete = new ConcurrentLinkedQueue<>();

    private final Object lock = new Object();


    public InMemoryCacheStoreProvider(Integer maxItems) {
        simpleCacheMap = Collections.synchronizedMap(new LRUMap<>(maxItems));
    }

    @Override
    public void clear() {
        synchronized (lock) {
            this.simpleCacheMap.clear();
        }
    }

    @Override
    public T put(K key, T value) {
        if (value == null) {
            remove(key);
        } else {
            synchronized (lock) {
                simpleCacheMap.put(key, new GtCacheObject<T>(value));
            }
        }
        return value;
    }

    @Override
    public boolean containsKey(K key) {
        synchronized (lock) {
            return simpleCacheMap.containsKey(key);
        }
    }

    @Override
    public Optional<T> get(K key) {
        synchronized (lock) {
            return Optional.ofNullable(simpleCacheMap.get(key))
                    .map(entry -> {
                        entry.setLastAccessed(System.currentTimeMillis());
                        return entry.getValue();
                    });
        }
    }

    @Override
    public long getLastAccess(K key) {
        synchronized (lock) {
            return Optional.ofNullable(simpleCacheMap
                    .get(key))
                    .map(entry -> entry.getLastAccessed())
                    .orElse(-1L);
        }
    }

    @Override
    public void remove(K key) {
        synchronized (lock) {
            simpleCacheMap.remove(key);
            Thread.yield();
        }
    }

    @Override
    public int size() {
        synchronized (lock) {
            return simpleCacheMap.size();
        }
    }

    private List<K> getKeysBefore(long time) {
        synchronized (lock) {
            return simpleCacheMap.entrySet().stream()
                    .filter(e -> time > e.getValue().getLastAccessed())
                    .map(e -> e.getKey())
                    .toList();
        }
    }

    @Async
    @Override
    public void cleanUp(long lastAccessed) {
        synchronized (lock) {
            keysToDelete.addAll(getKeysBefore(lastAccessed));
        }

        while (!this.keysToDelete.isEmpty()) {
            this.remove(this.keysToDelete.poll());
        }
    }
}
