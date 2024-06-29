package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.util.Collection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class CollectionPredicateBuilder {

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        var collectionPropertyName = path.getAlias();

        Path<?> parentPath = path.getParentPath();

        if (Root.class.isAssignableFrom(parentPath.getClass())) {
            path = ((Root<?>) parentPath).join(collectionPropertyName, JoinType.LEFT);
        } else {
            path = ((Join<?, ?>) parentPath).join(collectionPropertyName, JoinType.LEFT);
        }

        var predicate =
                builder.like(builder.upper(path.as(String.class)), "%" + value.toUpperCase() + "%");

        return predicate;
    }

    public static boolean isCollectionClass(Class<?> clazz) {
        return clazz.isAssignableFrom(Collection.class);
    }
}
