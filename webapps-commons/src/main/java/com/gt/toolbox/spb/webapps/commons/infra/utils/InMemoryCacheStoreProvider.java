package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.map.LRUMap;

public class InMemoryCacheStoreProvider<K, T> implements GtCacheStoreProvider<K, T> {

    private Map<K, GtCacheObject<K, T>> simpleCacheMap;

    @Override
    public void initialize(Integer maxItems) {
        simpleCacheMap = Collections.synchronizedMap(new LRUMap<>(maxItems));
    }

    @Override
    public void clear() {
        synchronized (simpleCacheMap) {
            this.simpleCacheMap.clear();
        }
    }

    @Override
    public void put(K key, GtCacheObject<K, T> value) {
        synchronized (simpleCacheMap) {
            simpleCacheMap.put(key, value);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return simpleCacheMap.containsKey(key);
    }

    @Override
    public Optional<GtCacheObject<K, T>> get(K key) {
        return Optional.ofNullable((GtCacheObject<K, T>) simpleCacheMap.get(key));
    }

    @Override
    public void remove(K key) {
        simpleCacheMap.remove(key);
    }

    @Override
    public int size() {
        return simpleCacheMap.size();
    }

    @Override
    public List<K> getKeysBefore(long time) {
        return this.simpleCacheMap.keySet().stream()
                .filter(k -> time > simpleCacheMap.get(k).lastAccessed)
                .toList();
    }

    @Override
    public void removeAll(Collection<K> keys) {
        synchronized (simpleCacheMap) {
            keys.forEach(k -> {
                this.simpleCacheMap.remove(k);
                // Se agrega esto para que le de prioridad a otros procesos
                Thread.yield();
            });
        }
    }

    @Override
    public void removeBefore(long l) {
        this.removeAll(this.getKeysBefore(l));

    }
}
