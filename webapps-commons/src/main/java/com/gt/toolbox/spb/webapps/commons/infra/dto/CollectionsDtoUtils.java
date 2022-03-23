package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class CollectionsDtoUtils {

    public static <T> Collection<T> synchronize(Collection<T> currentCollection, Collection<T> newCollection) {
        if (currentCollection == null) {
            return newCollection;
        }

        List<T> toRemove = findToRemove(currentCollection, newCollection);

        currentCollection.removeAll(toRemove);

        for (T item : newCollection) {
            if (!currentCollection.contains(item)) {
                currentCollection.add(item);
            }
        }

        return currentCollection;
    }

    public static <T> List<T> findToRemove(Collection<T> currentCollection, Collection<T> newCollection) {
        List<T> toRemove = new ArrayList<T>();

        for (T current : currentCollection) {
            if (!newCollection.contains(current)) {
                toRemove.add(current);
            }
        }
        return toRemove;
    }

    public static <E, D> Collection<E> synchronize(Collection<E> entityCollection, Collection<D> dtoCollection,
            DtoConverter<E, D> converter) {

        List<E> toRemove = findToRemove(entityCollection, dtoCollection, converter);

        entityCollection.removeAll(toRemove);

        if (dtoCollection != null) {
            for (D dto : dtoCollection) {
                // Busco el dto en la colección de entidades
                E entity = entityCollection.stream().filter(e -> converter.sameKey(e, dto)).findFirst().orElse(null);

                if (entity == null) {
                    // si no está creo la entidad y la agrego a la lista
                    entity = converter.toNewEntity(dto);
                    entityCollection.add(entity);
                } else {
                    // si está asigno los valores
                    converter.toEntity(entity, dto);
                }
            }
        }

        return entityCollection;

    }

    public static <E, D> List<E> findToRemove(Collection<E> entityCollection, Collection<D> dtoCollection,
            DtoConverter<E, D> converter) {
        List<E> toRemove = new ArrayList<>();

        for (E entity : entityCollection) {

            if (dtoCollection.stream().noneMatch(dto -> converter.sameKey(entity, dto))) {
                toRemove.add(entity);
            }
        }
        return toRemove;
    }

    public static <K, V> List<KeyValueDto<K, V>> of(Map<K, V> map) {
        List<KeyValueDto<K, V>> list = new ArrayList<>();
        map.entrySet().forEach(entry -> list.add(of(entry)));
        return list;
    }

    private static <V, K> KeyValueDto<K, V> of(Entry<K, V> entry) {
        return new KeyValueDto<K, V>(entry.getKey(), entry.getValue());
    }

    public static <K, V> Map<K, V> sinchronize(Map<K, V> map, List<KeyValueDto<K, V>> list) {
        List<K> keysToRemove = map.entrySet().stream().filter(
                par -> list.stream()
                        .noneMatch(localParam -> Objects.equals(localParam.getKey(), par.getKey())))
                .map(par -> par.getKey())
                .collect(Collectors.toList());

        keysToRemove.forEach(key -> map.remove(key));

        list.stream().filter(par -> !map.containsKey(par.getKey()))
                .forEach(par -> map.put(par.getKey(), par.getValue()));

        return map;
    }
}
