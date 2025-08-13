package com.gt.toolbox.spb.webapps.commons.infra.utils;

import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.lang.Nullable;

public class GtCacheObject<K, T> implements ValueWrapper {
    public long lastAccessed = System.currentTimeMillis();
    public T value;

    protected GtCacheObject(T value) {
        this.value = value;
    }

    @Nullable
    public Object get() {
        return value;
    }
}
