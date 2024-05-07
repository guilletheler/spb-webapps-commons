package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import lombok.Getter;


public class GtSpringCache<K, V> implements Cache {

    private final SimpleInMemoryCache<K, V> store;

    public GtSpringCache(String name, long secondsToLive, long secondsInterval, int maxItems) {
        this(name, secondsToLive, secondsInterval, maxItems, null);
    }

    public GtSpringCache(String name,
            long secondsToLive, long secondsInterval, int maxItems,
            Timer cleanupTimer) {
        this.name = name;
        store = new SimpleInMemoryCache<K, V>(secondsToLive, secondsInterval, maxItems,
                cleanupTimer);
    }

    @Getter
    String name;


    @SuppressWarnings("null")
    @Override
    public Object getNativeCache() {
        return store;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        @SuppressWarnings("unchecked")
        ValueWrapper ret = store.getWrapped((K) key);
        return ret;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        @SuppressWarnings("unchecked")
        var ret = (T) store.get((K) key);
        return ret;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        T toPut = null;
        try {
            toPut = valueLoader.call();

            @SuppressWarnings("unchecked")
            var kKey = (K) key;
            if (!store.contains(kKey)) {
                @SuppressWarnings("unchecked")
                var value = (V) toPut;
                store.put(kKey, value);
            }
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                    "error al almacenar valor en cache", e);
        }
        return toPut;
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        @SuppressWarnings("unchecked")
        var kKey = (K) key;
        @SuppressWarnings("unchecked")
        var vValue = (V) value;
        store.put(kKey, vValue);
    }


    @Override
    public void evict(@NonNull Object key) {
        @SuppressWarnings("unchecked")
        var kKey = (K) key;
        store.remove(kKey);
    }

    @Override
    public void clear() {
        store.clear();
    }

}
