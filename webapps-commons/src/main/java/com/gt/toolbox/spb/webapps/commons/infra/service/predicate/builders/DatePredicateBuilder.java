package com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

        long hyphenCount = value.chars().filter(ch -> ch == '-').count();
        boolean isSingleIsoDate = !value.contains("/") && (hyphenCount == 2 || (hyphenCount == 3 && (value.startsWith("=") || value.startsWith("<") || value.startsWith(">"))));

        var fromTo = new Predicate[] {null, null};
        if (value.startsWith("-")) {
            fromTo[1] =
                    buildSinglePredicate(builder, path, "<=" + value.substring(1));
        } else if (value.endsWith("-")) {
            fromTo[0] =
                    buildSinglePredicate(builder, path, ">=" + value.substring(1));
        } else if (value.contains("-") && !isSingleIsoDate) {
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

        return predicate;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Predicate buildSinglePredicate(CriteriaBuilder builder, Path<?> path,
            String value) {


        if (value != null && !value.isBlank()) {
            Class<?> type = path.getJavaType();

            ZonedDateTime tmpDateValue;
            String tmpString = "";

            try {
                Predicate predicate = null;
                if (value.startsWith("=")) {
                    tmpString = value.substring(1).trim();
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        Object converted = convertDateValue(tmpDateValue, type);
                        predicate = builder.equal(path, converted);
                    } else {
                        Expression<String> dateStringExpr =
                                builder.function("to_char", String.class,
                                        path, builder.literal("DD/MM/YYYY HH24:MI:SS"));
                        predicate = builder.like(dateStringExpr,
                                "%" + tmpString.toUpperCase() + "%");
                    }
                } else if (value.startsWith("<=")) {
                    tmpString = value.substring(2).trim();
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        Object converted = convertDateValue(tmpDateValue, type);
                        predicate = builder.lessThanOrEqualTo((Expression) path, (Comparable) converted);
                    }
                } else if (value.startsWith("<")) {
                    tmpString = value.substring(1).trim();
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        Object converted = convertDateValue(tmpDateValue, type);
                        predicate = builder.lessThan((Expression) path, (Comparable) converted);
                    }
                } else if (value.startsWith(">=")) {
                    tmpString = value.substring(2).trim();
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        Object converted = convertDateValue(tmpDateValue, type);
                        predicate = builder.greaterThanOrEqualTo((Expression) path, (Comparable) converted);
                    }
                } else if (value.startsWith(">")) {
                    tmpString = value.substring(1).trim();
                    tmpDateValue = parseZonedDateTime(tmpString);
                    if (tmpDateValue != null) {
                        Object converted = convertDateValue(tmpDateValue, type);
                        predicate = builder.greaterThan((Expression) path, (Comparable) converted);
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

    private static Object convertDateValue(ZonedDateTime zdt, Class<?> targetType) {
        if (targetType.equals(LocalDate.class)) {
            return zdt.toLocalDate();
        }
        if (targetType.equals(LocalDateTime.class)) {
            return zdt.toLocalDateTime();
        }
        if (targetType.equals(Date.class)) {
            return Date.from(zdt.toInstant());
        }
        if (targetType.equals(java.sql.Date.class)) {
            return java.sql.Date.valueOf(zdt.toLocalDate());
        }
        if (targetType.equals(java.sql.Timestamp.class)) {
            return java.sql.Timestamp.valueOf(zdt.toLocalDateTime());
        }
        if (targetType.equals(java.util.Calendar.class) || targetType.equals(java.util.GregorianCalendar.class)) {
            return java.util.GregorianCalendar.from(zdt);
        }
        return zdt;
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

        if (fecha.isBlank()) {
            return null;
        }

        if (!fecha.contains(":")) {
            if (!fecha.contains(" ")) {
                fecha = fecha + " 00";
            }
            fecha = fecha + ":00:00";
        } else if (StringUtils.countMatches(fecha, "") == 1) {
            fecha = fecha + ":00";
        }

        for (DateTimeFormatter dtf : new DateTimeFormatter[] {
                GtUtils.DTF_SLASH_DMYHMS, 
                GtUtils.DTF_SLASH_DMYYHMS,
                GtUtils.DTF_BAR_ISO_YYMDHMS
        }) {
            try {
                if (dtf == GtUtils.DTF_BAR_ISO_YYMDHMS) {
                    try {
                        var ldt = LocalDateTime.parse(fecha, dtf);
                        return ldt.atZone(ZoneId.systemDefault());
                    } catch (DateTimeParseException ex) {
                        var ld = LocalDate.parse(fecha.split(" ")[0], GtUtils.DTF_BAR_ISO_YYMD);
                        return ld.atStartOfDay(ZoneId.systemDefault());
                    }
                }
                LocalDate date = LocalDate.parse(fecha, dtf);
                var ret = date.atStartOfDay(ZoneId.systemDefault());
                return ret;
            } catch (DateTimeParseException ex) {
            }
        }

        return null;
    }
}
