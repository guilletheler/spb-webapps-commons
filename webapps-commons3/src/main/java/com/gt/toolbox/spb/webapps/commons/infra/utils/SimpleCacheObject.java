package com.gt.toolbox.spb.webapps.commons.infra.utils;

import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.lang.Nullable;

public class SimpleCacheObject<K, T> implements ValueWrapper {
    public long lastAccessed = System.currentTimeMillis();
    public T value;

    protected SimpleCacheObject(T value) {
        this.value = value;
    }

    @Nullable
    public Object get() {
        return value;
    }
}
