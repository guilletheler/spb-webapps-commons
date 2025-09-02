package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import java.util.Timer;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import lombok.Getter;


public class GtSpringCache<V> implements Cache {

    private static final Logger LOG =
            LoggerFactory.getLogger(GtSpringCache.class);

    private final GtCache<Object, V> store;

    public GtSpringCache(String name, long timeToLive, long cleanupInterval, int maxItems) {
        this(new InMemoryCacheStoreProvider<>(maxItems), name, timeToLive, cleanupInterval, null);
    }

    public GtSpringCache(String name, long timeToLive, long cleanupInterval, int maxItems,
            Timer cleanupTimer) {
        this(new InMemoryCacheStoreProvider<>(maxItems), name, timeToLive, cleanupInterval,
                cleanupTimer);
    }

    public GtSpringCache(GtCacheStoreProvider<Object, V> storeProvider, String name,
            long timeToLive, long cleanupInterval, Timer cleanupTimer) {
        this.name = name;
        store = new GtCache<Object, V>(storeProvider, timeToLive, cleanupInterval,
                cleanupTimer);
    }

    @Getter
    String name;

    @Override
    public @NonNull Object getNativeCache() {
        if (store == null) {
            throw new IllegalStateException("Cache is not initialized");
        }
        return store;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        var tmp = store.get(key);
        return new SimpleValueWrapper(tmp);
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        @SuppressWarnings("unchecked")
        var ret = (T) store.get(key);
        return ret;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        T toPut = null;
        try {
            if (store.contains(key)) {
                toPut = (T) store.get(key);
            } else {
                toPut = valueLoader.call();
                var value = (V) toPut;
                store.put(key, value);
            }
        } catch (Exception e) {
            LOG.error("error al almacenar valor en cache", e);
        }
        return toPut;
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        @SuppressWarnings("unchecked")
        var vValue = (V) value;
        store.put(key, vValue);
    }


    @Override
    public void evict(@NonNull Object key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }

}
