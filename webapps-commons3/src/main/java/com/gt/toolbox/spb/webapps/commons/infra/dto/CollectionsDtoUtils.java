package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gt.toolbox.spb.webapps.commons.infra.model.IWithId;

import org.springframework.data.repository.CrudRepository;

public class CollectionsDtoUtils {

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     * 
     * @param <T>
     * @param base
     * @param toSynch
     * @return
     */
    public static <T> Collection<T> synchronize(Collection<T> base, Collection<T> toSynch) {
        if (base == null) {
            return toSynch;
        }

        List<T> toRemove = findToRemove(base, toSynch);

        base.removeAll(toRemove);

        for (T item : toSynch) {
            if (!base.contains(item)) {
                base.add(item);
            }
        }

        return base;
    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     * 
     * @param <T>
     * @param base
     * @param toSynch
     * @return
     */
    public static <T> List<T> findToRemove(Collection<T> base, Collection<T> toSynch) {
        List<T> toRemove = new ArrayList<T>();

        for (T current : base) {
            if (!toSynch.contains(current)) {
                toRemove.add(current);
            }
        }
        return toRemove;
    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     * 
     * @param <E>
     * @param <D>
     * @param base
     * @param toSynch
     * @param converter
     * @return
     */
    public static <E, D> Collection<E> synchronize(Collection<E> base, Collection<D> toSynch,
            IDtoConverter<E, D> converter) {

        List<E> toRemove = findToRemove(base, toSynch, converter);

        base.removeAll(toRemove);

        if (toSynch != null) {
            for (D dto : toSynch) {

                // Busco el dto en la colección de entidades
                E entity = base.stream().filter(e -> converter.sameKey(e, dto)).findFirst()
                        .orElse(null);

                if (entity == null) {
                    // si no está creo la entidad y la agrego a la lista
                    entity = converter.toNewEntity(dto);
                    base.add(entity);
                } else {
                    // si está asigno los valores
                    converter.toEntity(entity, dto);
                }
            }
        }

        return base;

    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     * 
     * @param <ID>
     * @param <E>
     * @param <D>
     * @param base
     * @param toSynch
     * @param converter
     * @param repo
     * @return
     */
    public static <ID, E extends IWithId<ID>, D extends IWithId<ID>> Collection<E> synchronize(
            Collection<E> base, Collection<D> toSynch,
            IDtoConverter<E, D> converter, CrudRepository<E, ID> repo) {

        List<E> toRemove = findEntitiesToRemove(base, toSynch);

        base.removeAll(toRemove);

        if (toSynch != null) {
            for (D dto : toSynch) {

                // Busco el dto en la colección de entidades

                E entity = base.stream().filter(e -> e.getId().equals(dto.getId())).findFirst()
                        .orElse(null);

                if (entity == null) {
                    entity = repo.findById(dto.getId()).orElse(null);
                    if (entity != null) {
                        base.add(entity);
                    } else {
                        Logger.getLogger(CollectionsDtoUtils.class.getName()).warning(
                                "No se encontró la entidad con id " + dto.getId());
                    }
                }
            }
        }

        return base;

    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     * 
     * @param <E>
     * @param <D>
     * @param base
     * @param toSynch
     * @param converter
     * @return
     */
    public static <E, D> List<E> findToRemove(Collection<E> base, Collection<D> toSynch,
            IDtoConverter<E, D> converter) {
        List<E> toRemove = new ArrayList<>();

        for (E entity : base) {

            if (toSynch.stream().noneMatch(dto -> converter.sameKey(entity, dto))) {
                toRemove.add(entity);
            }
        }
        return toRemove;
    }

    public static <ID, E extends IWithId<ID>, D extends IWithId<ID>> List<E> findEntitiesToRemove(
            Collection<E> entityCollection, Collection<D> dtoCollection) {
        List<E> toRemove = new ArrayList<>();

        for (E entity : entityCollection) {

            if (dtoCollection.stream().noneMatch(dto -> entity.getId().equals(dto.getId()))) {
                toRemove.add(entity);
            }
        }
        return toRemove;
    }

    /**
     * Convierte un Map<K, V> en una List<KeyValueDto<K, V>>
     * 
     * @param <K>
     * @param <V>
     * @param map
     * @return
     */
    public static <K, V> List<KeyValueDto<K, V>> of(Map<K, V> map) {
        List<KeyValueDto<K, V>> list = new ArrayList<>();
        map.entrySet().forEach(entry -> list.add(of(entry)));
        return list;
    }

    /**
     * Convierte un Entry<K, V> en un KeyValueDto<K, V>
     * 
     * @param <V>
     * @param <K>
     * @param entry
     * @return
     */
    private static <V, K> KeyValueDto<K, V> of(Entry<K, V> entry) {
        return new KeyValueDto<K, V>(entry.getKey(), entry.getValue());
    }

    /**
     * Toma como base el Map<K, V> base e incorpora o quita la lista de List<KeyValueDto<K, V>>
     * toSynch
     * 
     * @param <K>
     * @param <V>
     * @param base
     * @param toSynch
     * @return
     */
    public static <K, V> Map<K, V> sinchronize(Map<K, V> base, List<KeyValueDto<K, V>> toSynch) {
        List<K> keysToRemove = base.entrySet().stream().filter(
                par -> toSynch.stream()
                        .noneMatch(localParam -> Objects.equals(localParam.getKey(), par.getKey())))
                .map(par -> par.getKey())
                .collect(Collectors.toList());

        keysToRemove.forEach(key -> base.remove(key));

        toSynch.forEach(par -> base.put(par.getKey(), par.getValue()));

        return base;
    }

    /**
     * Toma como base el Map<K, V> base e incorpora o quita la lista de List<KeyValueDto<K, V>>
     * toSynch
     * 
     * @param <K>
     * @param <V>
     * @param base
     * @param toSynch
     * @return
     */
    public static <K, V> Map<K, V> sinchronize(Map<K, V> base, Map<K, V> toSynch) {
        List<K> keysToRemove = base.entrySet().stream().filter(
                par -> toSynch.entrySet().stream()
                        .noneMatch(localParam -> Objects.equals(localParam.getKey(), par.getKey())))
                .map(par -> par.getKey())
                .collect(Collectors.toList());

        keysToRemove.forEach(key -> base.remove(key));

        toSynch.entrySet().forEach(par -> base.put(par.getKey(), par.getValue()));

        return base;
    }
}
