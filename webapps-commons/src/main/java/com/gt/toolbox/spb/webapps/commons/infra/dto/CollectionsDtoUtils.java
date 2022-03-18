package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CollectionsDtoUtils {

    public static <T> Collection<T> synchronize(Collection<T> currentCollection, Collection<T> newCollection) {
        if (currentCollection == null) {
            return newCollection;
        }

        List<T> toRemove = new ArrayList<T>();

        for (T current : currentCollection) {
            if (!newCollection.contains(current)) {
                toRemove.add(current);
            }
        }

        currentCollection.removeAll(toRemove);

        for (T item : newCollection) {
            if (!currentCollection.contains(item)) {
                currentCollection.add(item);
            }
        }

        return currentCollection;
    }

    public static <E, D> Collection<E> synchronize(Collection<E> entityCollection, Collection<D> dtoCollection, DtoConverter<E, D> converter) {
        
        List<E> toRemove = new ArrayList<>();

        for (E entity : entityCollection) {

            if(dtoCollection.stream().noneMatch(dto -> converter.sameKey(entity, dto))) {
                toRemove.add(entity);
            }
        }

        entityCollection.removeAll(toRemove);

        for (D dto : dtoCollection) {
            E entity = entityCollection.stream().filter(e -> converter.sameKey(e, dto)).findFirst().orElse(null);
            if(entity == null) {
                entity = converter.toNewEntity(dto);
                entityCollection.add(entity);
            } else {
                converter.toEntity(entity, dto);
            }
        }

        return entityCollection;
    }

    public static <K, V> List<KeyValueDto<K, V>> of(Map<K, V> map) {
        List<KeyValueDto<K, V>> list = new ArrayList<>();
        map.entrySet().forEach(entry -> list.add(of(entry)));
        return list;
    }

    private static <V, K> KeyValueDto<K, V> of(Entry<K, V> entry) {
        return new KeyValueDto<K, V>(entry.getKey(), entry.getValue());
    }
}
