package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.time.LocalTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Sirve para cualquier valor time: Time.class
 */
public class TimePredicateBuilder {

    private static final Logger LOG =
            LoggerFactory.getLogger(TimePredicateBuilder.class);

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        Predicate predicate = null;

        if (value != null && !value.isBlank()) {
            var fromTo = new Predicate[] {null, null};
            if (value.startsWith("-")) {
                fromTo[1] =
                        buildSinglePredicate(builder, path, "<=" + value.substring(1));
            } else if (value.endsWith("-")) {
                fromTo[0] =
                        buildSinglePredicate(builder, path, ">=" + value.substring(1));
            } else if (value.contains("-")) {
                var strFromTo = value.split("-");
                fromTo[0] =
                        buildSinglePredicate(builder, path, ">=" + strFromTo[0]);
                fromTo[1] =
                        buildSinglePredicate(builder, path, "<=" + strFromTo[1]);
            }

            if (fromTo[0] != null && fromTo[1] != null) {
                predicate = builder.and(fromTo[0], fromTo[1]);
            } else if (fromTo[0] != null) {
                predicate = fromTo[0];
            } else if (fromTo[1] != null) {
                predicate = fromTo[1];
            } else {
                predicate = buildSinglePredicate(builder, path, value);
            }
        }
        return predicate;
    }

    public static Predicate buildSinglePredicate(CriteriaBuilder builder, Path<?> path,
            String value) {

        if (value != null && !value.isBlank()) {
            Expression<LocalTime> dateExpression = path.as(LocalTime.class);

            LocalTime tmpDateValue;
            String tmpString = "";

            try {
                Predicate predicate = null;
                if (value.startsWith("=")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseLocalTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.equal(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith("<=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDateValue = parseLocalTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.lessThanOrEqualTo(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith("<")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseLocalTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.lessThan(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith(">=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDateValue = parseLocalTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.greaterThanOrEqualTo(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith(">")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseLocalTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.greaterThan(dateExpression, tmpDateValue);
                    }
                } else {
                    Expression<String> dateStringExpr =
                            builder.function("to_char", String.class,
                                    path, builder.literal("HH24:MI:SS"));

                    predicate = builder.like(dateStringExpr,
                            "%" + value.toUpperCase() + "%");
                }

                return predicate;
            } catch (NumberFormatException ex) {
                LOG.warn("Error obteniendo valores numericos de fecha", ex);
            }
        }

        return null;
    }

    public static boolean isTimeClass(Class<?> clazz) {
        return Objects.equals(LocalTime.class, clazz) || Objects.equals(java.sql.Time.class, clazz);
    }

    public static LocalTime parseLocalTime(String timeStr) {

        try {
            return LocalTime.parse(timeStr);
        } catch (Exception ex) {
            LOG.debug("No se puede convertir {} a Time", timeStr);
        }

        return null;
    }

}
