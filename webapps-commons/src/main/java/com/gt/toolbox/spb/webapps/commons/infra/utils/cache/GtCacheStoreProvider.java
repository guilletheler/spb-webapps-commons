package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import java.util.Optional;

/**
 * Almacena cada objeto agregando el timestamp de su último acceso
 */
public interface GtCacheStoreProvider<K, T> {

    void clear();

    T put(K key, T value);

    Optional<T> get(K key);

    long getLastAccess(K key);

    boolean containsKey(K key);

    void remove(K key);

    void cleanUp(long lastAccessed);

    int size();
}
