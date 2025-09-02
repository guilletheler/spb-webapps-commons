package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import lombok.Getter;
import lombok.Setter;

/**
 * ObjectWrapper for add lastAccessed
 */
public class GtCacheObject<T> implements GtLastAccessed {
    @Getter
    @Setter
    private long lastAccessed;

    @Getter
    private final T value;

    public GtCacheObject(T value) {
        this.value = value;
        this.lastAccessed = System.currentTimeMillis();
    }
}
