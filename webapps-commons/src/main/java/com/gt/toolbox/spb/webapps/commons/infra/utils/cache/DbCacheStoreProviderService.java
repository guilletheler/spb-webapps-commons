package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import java.util.Optional;
import java.util.Map.Entry;

/**
 * Defines the contract for a service that provides a persistent cache store,
 * typically backed by a
 * database. This interface abstracts the underlying data access mechanism for
 * cache operations.
 *
 * @param <K> The type of the keys maintained by this cache.
 * @param <V> The type of the cached values.
 */
public interface DbCacheStoreProviderService<K, V> {

    /**
     * Builds a new cache entity instance from a key and a value. The entity must
     * also track its
     * last access time.
     *
     * @param <T>   the type of the entity, which must extend {@link KeyValueDto}
     *              and
     *              {@link GtLastAccessed}
     * @param key   the key for the cache entry.
     * @param value the value for the cache entry.
     * @return a new entity instance, not yet persisted.
     */
    <T extends Entry<K, V> & GtLastAccessed> T buildEntity(K key, V value);

    /**
     * Saves or updates a given cache entity in the persistent store.
     *
     * @param <T>    the type of the entity to save.
     * @param entity the entity to be saved or updated.
     * @return the saved entity, which may have been updated by the persistence
     *         mechanism (e.g.,
     *         with a generated ID).
     */
    <T extends Entry<K, V> & GtLastAccessed> T save(T entity);

    /**
     * Retrieves a cache entity by its key.
     *
     * @param <T> the type of the entity to find.
     * @param key the key whose associated entity is to be returned.
     * @return an {@link Optional} containing the found entity, or
     *         {@link Optional#empty()} if no
     *         entity is found for the given key.
     */
    <T extends Entry<K, V> & GtLastAccessed> Optional<T> findByKey(K key);

    /**
     * Checks if the cache contains an entry for the specified key.
     *
     * @param key the key whose presence in this cache is to be tested.
     * @return {@code true} if this cache contains a mapping for the specified key,
     *         {@code false}
     *         otherwise.
     */
    boolean existsByKey(K key);

    /**
     * Removes all of the entries from this cache. The cache will be empty after
     * this call returns.
     */
    void clear();

    /**
     * Deletes the cache entry for a specified key.
     *
     * @param key the key whose mapping is to be removed from the cache.
     */
    void deleteByKey(K key);

    /**
     * Deletes the cache entry for a specified keys.
     *
     * @param lastAccessed the last accessed time to keep.
     */
    void deleteAllByKeyLessThan(long lastAccessed);

    /**
     * Returns the number of key-value mappings in this cache.
     *
     * @return the number of entries in the cache.
     */
    int count();

}
