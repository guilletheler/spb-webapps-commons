package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Sirve para cualquier valor decimal: BigDecimal, Double, Float
 */
public class DecimalPredicateBuilder {

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {

        Predicate predicate = null;

        if (value != null && !value.isBlank()) {

            Expression<BigDecimal> numberExpression = path.as(BigDecimal.class);

            BigDecimal tmpDoubleValue;
            String tmpString = "";

            try {
                if (value.startsWith("0") || value.startsWith("=")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
                    if (tmpDoubleValue != null) {
                        predicate = builder.equal(numberExpression, tmpDoubleValue);
                    }
                } else if (value.startsWith("<=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
                    if (tmpDoubleValue != null) {
                        predicate = builder.lessThanOrEqualTo(numberExpression, tmpDoubleValue);
                    }
                } else if (value.startsWith("<")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
                    if (tmpDoubleValue != null) {
                        predicate = builder.lessThan(numberExpression, tmpDoubleValue);
                    }
                } else if (value.startsWith(">=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
                    if (tmpDoubleValue != null) {
                        predicate = builder.greaterThanOrEqualTo(numberExpression, tmpDoubleValue);
                    }
                } else if (value.startsWith(">")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
                    if (tmpDoubleValue != null) {
                        predicate = builder.greaterThan(numberExpression, tmpDoubleValue);
                    }
                } else {
                    if (value.contains(".") && value.contains(",")
                            || StringUtils.countMatches(value, ".") > 1) {
                        tmpString = value.trim().replace(".", "");
                    }
                    tmpString = value.trim().replace(",", ".");
                    if (tmpString.contains(".")) {
                        tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
                        if (tmpDoubleValue != null) {
                            predicate = builder.equal(numberExpression, tmpDoubleValue);
                        }
                    } else {
                        tmpString = "%" + value.trim() + "%";
                        predicate = builder.like(path.as(String.class), tmpString);
                    }
                }

            } catch (NumberFormatException ex) {

            }
        }

        return predicate;
    }

    public static boolean isDecimalClass(Class<?> clazz) {
        return Objects.equals(Float.class, clazz) || Objects.equals(float.class, clazz)
                || Objects.equals(Double.class, clazz) || Objects.equals(double.class, clazz)
                || Objects.equals(BigDecimal.class, clazz);
    }
}
