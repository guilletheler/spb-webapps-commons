package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GtCacheStoreProvider<K, T> {
    void initialize(Integer maxItems);

    void clear();

    void put(K key, GtCacheObject<K, T> value);

    Optional<GtCacheObject<K, T>> get(K key);

    boolean containsKey(K key);

    List<K> getKeysBefore(long l);

    void remove(K key);

    void removeAll(Collection<K> keys);

    void removeBefore(long l);

    int size();
}
