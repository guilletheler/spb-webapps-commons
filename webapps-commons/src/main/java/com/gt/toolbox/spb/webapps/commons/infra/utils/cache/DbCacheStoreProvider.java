package com.gt.toolbox.spb.webapps.commons.infra.utils.cache;

import java.util.Optional;
import org.springframework.scheduling.annotation.Async;
import com.gt.toolbox.spb.webapps.commons.infra.dto.KeyValueDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbCacheStoreProvider<K, V, T extends KeyValueDto<K, V> & GtLastAccessed>
        implements GtCacheStoreProvider<K, V> {

    private final DbCacheStoreProviderService<K, V> storeService;

    @Override
    public void clear() {
        storeService.clear();
    }

    @Override
    public V put(K key, V value) {
        var entity = storeService.findByKey(key)
                .orElseGet(() -> storeService.buildEntity(key, value));
        entity.setValue(value);
        entity.setLastAccessed(System.currentTimeMillis());
        return storeService.save(entity).getValue();
    }

    @Override
    public Optional<V> get(K key) {
        return storeService.findByKey(key)
                .map(keyValue -> {
                    keyValue.setLastAccessed(System.currentTimeMillis());
                    return keyValue.getValue();
                });
    }

    @Override
    public long getLastAccess(K key) {
        return storeService.findByKey(key)
                .map(val -> val.getLastAccessed())
                .orElse(-1L);
    }

    @Override
    public boolean containsKey(K key) {
        return storeService.existsByKey(key);
    }

    @Override
    public void remove(K key) {
        storeService.deleteByKey(key);
    }

    @Async
    @Override
    public void cleanUp(long lastAccessed) {
        storeService.deleteAllByKeyLessThan(lastAccessed);
    }

    @Override
    public int size() {
        return storeService.count();
    }
}
