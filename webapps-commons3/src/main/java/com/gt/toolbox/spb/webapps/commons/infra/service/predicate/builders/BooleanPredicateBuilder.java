package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.util.Objects;
import java.util.Optional;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class BooleanPredicateBuilder {

    private static Optional<Boolean> parseBoolean(String value) {
        var valor = value.trim().equalsIgnoreCase("si")
                || value.trim().equalsIgnoreCase("true");
        if (valor) {
            return Optional.of(true);
        }

        valor = value.trim().equalsIgnoreCase("no")
                || value.trim().equalsIgnoreCase("false");

        if (valor) {
            return Optional.of(false);
        }

        return Optional.empty();
    }

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        Predicate predicate = null;

        if (value != null && !value.isBlank()) {
            var val = parseBoolean(value);

            if (val.isPresent()) {
                if (val.get()) {
                    predicate = builder.equal(builder.coalesce(path, Boolean.FALSE), Boolean.TRUE);
                } else {
                    predicate = builder.equal(builder.coalesce(path, Boolean.FALSE), Boolean.FALSE);
                }
            }
        }
        return predicate;
    }

    public static boolean isBooleanClass(Class<?> clazz) {
        return Objects.equals(clazz, boolean.class)
                || Objects.equals(clazz, Boolean.class);

    }
}
