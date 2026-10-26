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
        return buildPredicate(null, builder, path, value);
    }

    public static Predicate buildPredicate(jakarta.persistence.criteria.CriteriaQuery<?> query, CriteriaBuilder builder, Path<?> path, String value) {
        var collectionPropertyName = path.getAlias();
        if (collectionPropertyName != null && collectionPropertyName.contains("_")) {
            collectionPropertyName = collectionPropertyName.substring(collectionPropertyName.lastIndexOf('_') + 1);
        }

        Path<?> parentPath = path.getParentPath();

        if (query != null && parentPath instanceof jakarta.persistence.criteria.From<?, ?> from) {
            jakarta.persistence.criteria.Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<?> subRoot = subquery.from(from.getJavaType());
            Join<?, ?> subJoin = subRoot.join(collectionPropertyName, JoinType.INNER);

            Predicate correlation = builder.equal(subRoot, from);
            Predicate match = builder.like(builder.upper(subJoin.as(String.class)), "%" + value.toUpperCase() + "%");

            subquery.select(builder.literal(1))
                    .where(builder.and(correlation, match));

            return builder.exists(subquery);
        }

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
