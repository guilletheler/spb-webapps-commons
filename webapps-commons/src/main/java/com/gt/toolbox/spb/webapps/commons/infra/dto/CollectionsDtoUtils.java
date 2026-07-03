package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

public class CollectionsDtoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CollectionsDtoUtils.class);

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     * 
     * @param <T>
     * @param base
     * @param toSynch
     * @return
     */
    public static <T> Collection<T> synchronize(Collection<T> base, Collection<T> toSynch) {
        if (base != null && toSynch != null) {

            List<T> toRemove = findToRemove(base, toSynch);

            base.removeAll(toRemove);

            for (T item : toSynch) {
                if (!base.contains(item)) {
                    base.add(item);
                }
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
     */
    public static <E, D, EL extends Collection<E>, DL extends Collection<D>> EL synchronize(EL base, DL toSynch,
            BiPredicate<E, D> idComparator,
            Function<D, E> toNew,
            BiConsumer<D, E> update) {

        List<E> toRemove = findToRemove(base, toSynch, idComparator);

        base.removeAll(toRemove);

        if (toSynch != null) {
            for (D dto : toSynch) {

                // Busco el dto en la colección de entidades
                E entity = base.stream().filter(e -> idComparator.test(e, dto)).findFirst()
                        .orElse(null);

                if (entity == null) {
                    // si no está creo la entidad y la agrego a la lista
                    entity = toNew.apply(dto);
                    base.add(entity);
                } else {
                    // si está asigno los valores
                    update.accept(dto, entity);
                }
            }
        }

        return base;
    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     */
    public static <E, D> Collection<E> synchronize(Collection<E> base, Collection<D> toSynch,
            Function<E, D> toDto,
            Function<D, E> toNew,
            BiConsumer<D, E> update) {

        BiPredicate<E, D> idComparator = new ObjectsIdComparator<E, D>();

        return synchronize(base, toSynch, idComparator, toNew, update);
    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     */
    public static <ID, E, D> Collection<E> synchronize(
            Collection<E> base, Collection<D> toSynch,
            CrudRepository<E, ID> repo) {
        var idComparator = new ObjectsIdComparator<E, D>();
        var idResolver = new ObjectIdResolver<D, ID>();

        return synchronize(base, toSynch, idComparator, idResolver, repo);
    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     */
    public static <ID, E, D> Collection<E> synchronize(
            Collection<E> base, Collection<D> toSynch,
            BiPredicate<E, D> idComparator,
            Function<D, ID> idResolver,
            CrudRepository<E, ID> repo) {

        List<E> toRemove = findEntitiesToRemove(base, toSynch, idComparator);

        base.removeAll(toRemove);

        if (toSynch != null) {
            for (D dto : toSynch) {

                // Busco el dto en la colección de entidades

                E entity = base.stream().filter(e -> idComparator.test(e, dto)).findFirst()
                        .orElse(null);

                if (entity == null) {
                    var dtoId = idResolver.apply(dto);
                    if (dtoId != null) {
                        entity = repo.findById(dtoId).orElse(null);
                    }
                    if (entity != null) {
                        base.add(entity);
                    } else {
                        LOG.warn("No se encontró la entidad con id {}", dtoId);
                    }
                }
            }
        }

        return base;

    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     */
    public static <E, D> List<E> findToRemove(Collection<E> base, Collection<D> toSynch,
            BiPredicate<E, D> idComparator) {
        List<E> toRemove = new ArrayList<>();

        if (base != null && toSynch != null) {
            for (E entity : base) {
                if (toSynch.stream().noneMatch(dto -> idComparator.test(entity, dto))) {
                    toRemove.add(entity);
                }
            }
        }
        return toRemove;
    }

    /**
     * Toma como base la colección base e incorpora o quita la colección toSynch
     */
    public static <E, D> List<E> findToRemove(Collection<E> base, Collection<D> toSynch, Function<E, D> toDto) {
        return findToRemove(base, toSynch, new ObjectsIdComparator<>());
    }

    public static <E, D> List<E> findEntitiesToRemove(Collection<E> entityCollection, Collection<D> dtoCollection) {
        return findToRemove(entityCollection, dtoCollection, new ObjectsIdComparator<>());
    }

    public static <E, D> List<E> findEntitiesToRemove(
            Collection<E> entityCollection, Collection<D> dtoCollection,
            BiPredicate<E, D> idComparator) {
        List<E> toRemove = new ArrayList<>();

        for (E entity : entityCollection) {

            if (dtoCollection.stream()
                    .noneMatch(dto -> idComparator.test(entity, dto))) {
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
    public static <K, V> List<Entry<K, V>> of(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>();
        map.entrySet().forEach(entry -> list.add(entry));
        return list;
    }

    /**
     * Toma como base el Map<K, V> base e incorpora o quita la lista de
     * List<KeyValueDto<K, V>>
     * toSynch
     * 
     * @param <K>
     * @param <V>
     * @param base
     * @param toSynch
     * @return
     */
    public static <K, V> Map<K, V> synchronize(Map<K, V> base, List<Entry<K, V>> toSynch) {
        if (toSynch != null) {

            List<K> keysToRemove = base.entrySet().stream().filter(
                    par -> toSynch.stream()
                            .noneMatch(localParam -> Objects.equals(localParam.getKey(),
                                    par.getKey())))
                    .map(par -> par.getKey())
                    .collect(Collectors.toList());

            keysToRemove.forEach(key -> base.remove(key));

            toSynch.forEach(par -> base.put(par.getKey(), par.getValue()));
        }

        return base;
    }

    /**
     * Toma como base el Map<K, V> base e incorpora o quita la lista de
     * List<KeyValueDto<K, V>>
     * toSynch
     * 
     * @param <K>
     * @param <V>
     * @param base
     * @param toSynch
     * @return
     */
    public static <K, V> Map<K, V> synchronize(Map<K, V> base, Map<K, V> toSynch) {

        if (toSynch != null) {

            List<K> keysToRemove = base.entrySet().stream().filter(
                    par -> toSynch.entrySet().stream()
                            .noneMatch(localParam -> Objects.equals(localParam.getKey(),
                                    par.getKey())))
                    .map(par -> par.getKey())
                    .collect(Collectors.toList());

            keysToRemove.forEach(key -> base.remove(key));

            toSynch.entrySet().forEach(par -> base.put(par.getKey(), par.getValue()));
        }

        return base;
    }

    public static <K, V> Map<K, V> cloneMap(Map<K, V> original) {
        Map<K, V> ret = new HashMap<>();
        if (original != null) {

            original.forEach((k, v) -> {
                ret.put(k, v);
            });
        }
        return ret;
    }

    public static <T> List<T> cloneList(List<T> original) {
        List<T> ret = new ArrayList<>();

        if (original != null) {
            ret.addAll(original);
        }

        return ret;
    }

    public static <T> Set<T> cloneSet(Set<T> original) {
        Set<T> ret = new HashSet<>();

        if (original != null) {
            ret.addAll(original);
        }
        return ret;
    }
}
