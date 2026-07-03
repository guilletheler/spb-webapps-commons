package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class StringPredicateBuilder {

    private static final Logger LOG =
            LoggerFactory.getLogger(StringPredicateBuilder.class);

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        return buildPredicate(builder, path, value, "==");
    }

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value, String operator) {
        Predicate predicate = null;

        Expression<String> expr = path.as(String.class);

        boolean isExact = false;
        if (value.length() > 1 && value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
            isExact = true;
        }

        if ("=ilike=".equals(operator)) {
            predicate = builder.like(builder.upper(expr), value.toUpperCase());
        } else if ("=like=".equals(operator)) {
            predicate = builder.like(expr, value);
        } else {
            if (isExact) {
                LOG.info("Igualando string en campo " + path.getAlias() + " con " + value);
                predicate = builder.like(expr, value);
            } else {
                predicate = builder.like(builder.upper(expr), "%" + value.toUpperCase() + "%");
            }
        }

        return predicate;
    }
}
