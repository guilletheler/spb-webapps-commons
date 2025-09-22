package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class StringPredicateBuilder {

    private static final Logger LOG =
            LoggerFactory.getLogger(DatePredicateBuilder.class);

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        Predicate predicate = null;

        Expression<String> expr = path.as(String.class);

        if (value.length() > 1 && value.startsWith("'")) {
            value = value.substring(1);
            if (value.endsWith("'")) {
                value = value.substring(0, value.length() - 1);
            }
            LOG.info("Igualando string en campo " + path.getAlias() + " con " + value);

            predicate = builder.like(expr,
                    value);
        } else {
            predicate = builder.like(builder.upper(expr),
                    "%" + value.toUpperCase() + "%");
        }

        return predicate;
    }

}
