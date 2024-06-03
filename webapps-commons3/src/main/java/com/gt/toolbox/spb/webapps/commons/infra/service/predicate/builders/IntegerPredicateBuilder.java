package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.math.BigInteger;
import java.util.Objects;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Sirve para cualquier valor entero: BigInteger, Integer, Long, Short, Byte
 */
public class IntegerPredicateBuilder {

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {

        Predicate predicate = null;

        if (value != null && !value.isBlank()) {

            Expression<BigInteger> numberExpression = path.as(BigInteger.class);

            BigInteger tmpLongValue;
            String tmpString = "";

            try {
                if (value.startsWith("0") || value.startsWith("=")) {
                    tmpString = value.substring(1).trim();
                    tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
                    if (tmpLongValue != null) {
                        predicate = builder.equal(numberExpression, tmpLongValue);
                    }
                } else if (value.startsWith("<=")) {
                    tmpString = value.substring(2).trim();
                    tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
                    if (tmpLongValue != null) {
                        predicate = builder.lessThanOrEqualTo(numberExpression, tmpLongValue);
                    }
                } else if (value.startsWith("<")) {
                    tmpString = value.substring(1).trim();
                    tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
                    if (tmpLongValue != null) {
                        predicate = builder.lessThan(numberExpression, tmpLongValue);
                    }
                } else if (value.startsWith(">=")) {
                    tmpString = value.substring(2).trim();
                    tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
                    if (tmpLongValue != null) {
                        predicate = builder.greaterThanOrEqualTo(numberExpression, tmpLongValue);
                    }
                } else if (value.startsWith(">")) {
                    tmpString = value.substring(1).trim();
                    tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
                    if (tmpLongValue != null) {
                        predicate = builder.greaterThan(numberExpression, tmpLongValue);
                    }
                } else {
                    tmpString = value.trim().replace(".", "");
                    tmpString = "%" + value.trim() + "%";
                    predicate = builder.like(path.as(String.class), tmpString);
                }

            } catch (NumberFormatException ex) {

            }
        }

        return predicate;
    }

    public static boolean isIntegerClass(Class<?> clazz) {
        return Objects.equals(Byte.class, clazz) || Objects.equals(byte.class, clazz)
                || Objects.equals(Integer.class, clazz) || Objects.equals(int.class, clazz)
                || Objects.equals(Long.class, clazz) || Objects.equals(long.class, clazz)
                || Objects.equals(BigInteger.class, clazz);
    }
}
