package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gt.toolbox.spb.webapps.commons.infra.utils.GtUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Sirve para cualquier valor de fecha: Date.class, java.sql.Date.class, Calendar.class,
 * GregorianCalendar.class, LocalDate.class, LocalDateTime.class, ZonedDateTime.class
 */
public class DatePredicateBuilder {

    private static final Logger LOG =
            LoggerFactory.getLogger(DatePredicateBuilder.class);

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
            Expression<ZonedDateTime> dateExpression = path.as(ZonedDateTime.class);

            ZonedDateTime tmpDateValue;
            String tmpString = "";

            try {
                Predicate predicate = null;
                if (value.startsWith("=")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.equal(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith("<=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.lessThanOrEqualTo(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith("<")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.lessThan(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith(">=")) {
                    tmpString = value.substring(2).trim().replace(",", ".");
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.greaterThanOrEqualTo(dateExpression, tmpDateValue);
                    }
                } else if (value.startsWith(">")) {
                    tmpString = value.substring(1).trim().replace(",", ".");
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        predicate = builder.greaterThan(dateExpression, tmpDateValue);
                    }
                } else {
                    Expression<String> dateStringExpr =
                            builder.function("to_char", String.class,
                                    path, builder.literal("DD/MM/YYYY HH24:MI:SS"));

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

    public static boolean isDateClass(Class<?> clazz) {
        return Objects.equals(Date.class, clazz) || Objects.equals(java.sql.Date.class, clazz)
                || Objects.equals(Calendar.class, clazz)
                || Objects.equals(GregorianCalendar.class, clazz)
                || Objects.equals(LocalDate.class, clazz)
                || Objects.equals(LocalDateTime.class, clazz)
                || Objects.equals(ZonedDateTime.class, clazz);
    }

    public static Date parseDate(String fecha) {

        for (SimpleDateFormat sdf : GtUtils.DATE_FORMATS) {
            try {
                Date ret = sdf.parse(fecha);
                return ret;
            } catch (ParseException ex) {
                LOG.debug("No se puede convertir {} a Date", fecha);
            }
        }

        return null;
    }

    public static LocalDate parseLocalDate(String fecha) {
        for (DateTimeFormatter sdf : GtUtils.LOCAL_DATE_FORMATS) {
            try {
                var ret = LocalDate.parse(fecha, sdf);
                return ret;
            } catch (DateTimeParseException ex) {
                LOG.debug("No se puede convertir {} a LocalDate", fecha);
            }
        }

        return null;
    }

    public static ZonedDateTime parseZonedDateTime(String fecha) {

        if (!fecha.contains(":")) {
            if (!fecha.contains(" ")) {
                fecha = fecha + " 00";
            }
            fecha = fecha + ":00:00";
        } else if (StringUtils.countMatches(fecha, "") == 1) {
            fecha = fecha + ":00";
        }

        var formats = GtUtils.LOCAL_DATE_TIME_FORMATS;
        formats = new DateTimeFormatter[] {GtUtils.DTF_SLASH_DMYHMS, GtUtils.DTF_SLASH_DMYYHMS};
        for (DateTimeFormatter dtf : formats) {
            try {
                var parsed = dtf.parse(fecha);
                var ret = ZonedDateTime.from(parsed);
                return ret;
            } catch (DateTimeParseException ex) {
            }
        }

        return null;
    }
}
