package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class EnumPredicateBuilder {

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        Predicate predicate;
        if (value.length() > 1 && value.startsWith("'")
                && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);

            predicate = builder.like(path.as(String.class),
                    value);
        } else {
            predicate = builder.like(builder.upper(path.as(String.class)),
                    "%" + value.toUpperCase() + "%");
        }

        return predicate;
    }

}
