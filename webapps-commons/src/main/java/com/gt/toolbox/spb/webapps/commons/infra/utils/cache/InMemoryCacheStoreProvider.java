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

    public InMemoryCacheStoreProvider(Integer maxItems) {
        simpleCacheMap = Collections.synchronizedMap(new LRUMap<>(maxItems));
    }

    @Override
    public void clear() {
        var s = simpleCacheMap.keySet();
        synchronized (s) {
            this.simpleCacheMap.clear();
        }
    }

    @Override
    public T put(K key, T value) {
        simpleCacheMap.put(key, new GtCacheObject<T>(value));
        return value;
    }

    @Override
    public boolean containsKey(K key) {
        return simpleCacheMap.containsKey(key);
    }

    @Override
    public Optional<T> get(K key) {
        return Optional.ofNullable(simpleCacheMap.get(key))
                .map(entry -> {
                    entry.setLastAccessed(System.currentTimeMillis());
                    return entry.getValue();
                });
    }

    @Override
    public long getLastAccess(K key) {
        return Optional.ofNullable(simpleCacheMap
                .get(key))
                .map(entry -> entry.getLastAccessed())
                .orElse(-1L);
    }

    @Override
    public void remove(K key) {
        simpleCacheMap.remove(key);
    }

    @Override
    public int size() {
        return simpleCacheMap.size();
    }

    private List<K> getKeysBefore(long time) {
        var s = simpleCacheMap.keySet();
        synchronized (s) {
            return s.stream()
                    .filter(k -> time > simpleCacheMap.get(k).getLastAccessed())
                    .toList();
        }
    }

    @Async
    @Override
    public void cleanUp(long lastAccessed) {
        keysToDelete.addAll(getKeysBefore(lastAccessed));

        while (!this.keysToDelete.isEmpty()) {
            this.remove(this.keysToDelete.poll());
        }
    }
}
