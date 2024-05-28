package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import com.gt.toolbox.spb.webapps.commons.infra.utils.Utils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Sirve para cualquier valor decimal: BigDecimal, Double, Float
 */
public class DatePredicateBuilder {

    public static Predicate buildPredicate(CriteriaBuilder builder, Path<?> path, String value) {
        Predicate predicate = null;

        if (value != null && !value.isBlank()) {
            var fromTo = new Predicate[] {null, null};
            if (value.startsWith("-")) {
                fromTo[1] =
                        buildSinglePredicate(builder, path, "<=" + value.substring(1)).orElse(null);
            } else if (value.endsWith("-")) {
                fromTo[0] =
                        buildSinglePredicate(builder, path, ">=" + value.substring(1)).orElse(null);
            } else if (value.contains("-")) {
                var strFromTo = value.split("-");
                fromTo[0] =
                        buildSinglePredicate(builder, path, ">=" + strFromTo[0]).orElse(null);
                fromTo[1] =
                        buildSinglePredicate(builder, path, "<=" + strFromTo[1]).orElse(null);
            }

            if (fromTo[0] != null && fromTo[1] != null) {
                predicate = builder.and(fromTo[0], fromTo[1]);
            } else if (fromTo[0] != null) {
                predicate = fromTo[0];
            } else if (fromTo[1] != null) {
                predicate = fromTo[1];
            } else {
                predicate = buildSinglePredicate(builder, path, value).orElse(null);
            }
        }
        return predicate;
    }

    public static Optional<Predicate> buildSinglePredicate(CriteriaBuilder builder, Path<?> path,
            String value) {

        if (value != null && !value.isBlank()) {

            Expression<LocalDateTime> dateExpression = path.as(LocalDateTime.class);

            LocalDateTime tmpDateValue;
            String tmpString = "";

            try {
                Predicate predicate = null;
                if (value.startsWith("0") || value.startsWith("=")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseLocalDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.equal(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith("<=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDateValue = parseLocalDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.lessThanOrEqualTo(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith("<")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseLocalDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.lessThan(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith(">=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDateValue = parseLocalDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.greaterThanOrEqualTo(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith(">")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseLocalDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.greaterThan(dateExpression, tmpDateValue);
                    }
                } else {
                    tmpString = value.trim().replace(",", ".");
                    tmpDateValue = parseLocalDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.equal(dateExpression, tmpDateValue);
                    }
                }

                // Logger.getLogger(DatePredicateBuilder.class.getName()).log(Level.INFO, tmpString
                // + " -> " +
                // Optional.ofNullable(tmpDateValue).map(d -> d.toString()).orElse(""));

                return Optional.ofNullable(predicate);
            } catch (NumberFormatException ex) {

            }
        }

        return Optional.empty();
    }

    public static boolean isDateClass(Class<?> clazz) {
        return Objects.equals(Date.class, clazz) || Objects.equals(java.sql.Date.class, clazz)
                || Objects.equals(Calendar.class, clazz)
                || Objects.equals(GregorianCalendar.class, clazz)
                || Objects.equals(LocalDate.class, clazz) || Objects.equals(LocalTime.class, clazz)
                || Objects.equals(LocalDateTime.class, clazz);
    }

    public static Date parseDate(String fecha) {

        for (SimpleDateFormat sdf : Utils.DATE_FORMATS) {
            try {
                Date ret = sdf.parse(fecha);
                return ret;
            } catch (ParseException ex) {
                // Logger.getLogger(QueryBundle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    public static LocalDate parseLocalDate(String fecha) {
        for (DateTimeFormatter sdf : Utils.LOCAL_DATE_FORMATS) {
            try {
                var ret = LocalDate.parse(fecha, sdf);
                return ret;
            } catch (DateTimeParseException ex) {
                // Logger.getLogger(QueryBundle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    public static LocalDateTime parseLocalDateTime(String fecha) {

        if (!fecha.contains(":")) {
            if (!fecha.contains(" ")) {
                fecha = fecha + " 00";
            }
            fecha = fecha + ":00:00";
        } else if (StringUtils.countMatches(fecha, "") == 1) {
            fecha = fecha + ":00";
        }

        var formats = Utils.LOCAL_DATE_TIME_FORMATS;
        formats = new DateTimeFormatter[] {Utils.DTF_SLASH_DMYHMS, Utils.DTF_SLASH_DMYYHMS};
        for (DateTimeFormatter dtf : formats) {
            try {
                var parsed = dtf.parse(fecha);
                var ret = LocalDateTime.from(parsed);
                return ret;
            } catch (DateTimeParseException ex) {
            }
        }

        return null;
    }
}
