package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class RsqlSpecificationArgumentResolver implements HandlerMethodArgumentResolver {

    private static class FieldMeta {
        final Class<?> type;
        final boolean isCollection;
        final boolean isMap;

        FieldMeta(Class<?> type, boolean isCollection, boolean isMap) {
            this.type = type;
            this.isCollection = isCollection;
            this.isMap = isMap;
        }
    }

    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
            "([a-zA-Z0-9_.]+)(==|=like=|=ilike=|!=|=gt=|=lt=|=ge=|=le=|=in=|=out=)('[^']*'|\\\"[^\\\"]*\\\"|\\([^)]*\\)|[^\\s,;()]+)");

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return Specification.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) throws Exception {
        String filter = webRequest.getParameter("filter");
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }

        Class<?> entityClass = ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve();
        if (entityClass == null) {
            return null;
        }

        return parseFilter(filter, entityClass);
    }

    private static Specification<Object> parseFilter(String filter, Class<?> entityClass) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }

        String trimmed = filter.trim();
        while (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            int len = trimmed.length();
            int level = 0;
            boolean matching = true;
            for (int i = 0; i < len - 1; i++) {
                char c = trimmed.charAt(i);
                if (c == '(') {
                    level++;
                } else if (c == ')') {
                    level--;
                    if (level == 0) {
                        matching = false;
                        break;
                    }
                }
            }
            if (matching) {
                trimmed = trimmed.substring(1, len - 1).trim();
            } else {
                break;
            }
        }

        if (trimmed.isEmpty()) {
            return null;
        }

        List<String> orParts = splitTopLevel(trimmed, ',');
        if (orParts.size() > 1) {
            Specification<Object> spec = null;
            for (String part : orParts) {
                Specification<Object> partSpec = parseFilter(part, entityClass);
                if (partSpec != null) {
                    if (spec == null) {
                        spec = partSpec;
                    } else {
                        spec = spec.or(partSpec);
                    }
                }
            }
            return spec;
        }

        List<String> andParts = splitTopLevel(trimmed, ';');
        if (andParts.size() > 1) {
            Specification<Object> spec = null;
            for (String part : andParts) {
                Specification<Object> partSpec = parseFilter(part, entityClass);
                if (partSpec != null) {
                    if (spec == null) {
                        spec = partSpec;
                    } else {
                        spec = spec.and(partSpec);
                    }
                }
            }
            return spec;
        }

        Matcher matcher = COMPARISON_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String field = matcher.group(1);
            String operator = matcher.group(2);
            String value = matcher.group(3);

            FieldMeta meta = getFieldMeta(entityClass, field);
            if (meta != null) {
                return new Specification<Object>() {
                    @Override
                    public Predicate toPredicate(@NonNull Root<Object> root,
                            @Nullable CriteriaQuery<?> query,
                            @NonNull CriteriaBuilder cb) {
                        try {
                            Expression<?> path = root;
                            String[] parts = field.split("\\.");
                            for (int i = 0; i < parts.length; i++) {
                                String part = parts[i];
                                if (i == parts.length - 1) {
                                    if (meta.isCollection) {
                                        path = ((From<?, ?>) path).join(part, JoinType.LEFT);
                                    } else if (meta.isMap) {
                                        path = ((From<?, ?>) path).joinMap(part, JoinType.LEFT).value();
                                    } else {
                                        path = ((Path<?>) path).get(part);
                                    }
                                } else {
                                    path = ((From<?, ?>) path).join(part, JoinType.LEFT);
                                }
                            }
                            return buildPredicate((Path<?>) path, cb, operator, value, meta);
                        } catch (Exception e) {
                            System.err.println("Error creating predicate for field: " + field);
                            e.printStackTrace();
                            return cb.disjunction();
                        }
                    }
                };
            }
        }

        return null;
    }

    private static List<String> splitTopLevel(String query, char targetChar) {
        List<String> parts = new ArrayList<>();
        int parenthesisLevel = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int lastStart = 0;
        int len = query.length();
        for (int i = 0; i < len; i++) {
            char c = query.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenthesisLevel++;
                } else if (c == ')') {
                    parenthesisLevel--;
                } else if (c == targetChar && parenthesisLevel == 0) {
                    parts.add(query.substring(lastStart, i));
                    lastStart = i + 1;
                }
            }
        }
        parts.add(query.substring(lastStart));
        return parts;
    }

    private static boolean isNumericType(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (Number.class.isAssignableFrom(type)) {
            return true;
        }
        return type == int.class || type == long.class || type == double.class ||
                type == float.class || type == short.class || type == byte.class;
    }

    private static Predicate buildPredicate(Path<?> path, CriteriaBuilder cb, String operator, String value,
            FieldMeta meta) {
        String cleanVal = value;
        if (cleanVal.startsWith("'") && cleanVal.endsWith("'")) {
            cleanVal = cleanVal.substring(1, cleanVal.length() - 1);
        } else if (cleanVal.startsWith("\"") && cleanVal.endsWith("\"")) {
            cleanVal = cleanVal.substring(1, cleanVal.length() - 1);
        }

        if (meta.type.isEnum()) {
            List<Object> matchingEnums = new ArrayList<>();
            String lowerPattern = cleanVal.replace("*", "").replace("%", "").toLowerCase();
            boolean hasWildcard = cleanVal.contains("*") || cleanVal.contains("%") || operator.equals("=like=") || operator.equals("=ilike=");
            for (Object enumConstant : meta.type.getEnumConstants()) {
                String name = ((Enum<?>) enumConstant).name().toLowerCase();
                if (hasWildcard) {
                    if (name.contains(lowerPattern)) {
                        matchingEnums.add(enumConstant);
                    }
                } else {
                    if (name.equals(lowerPattern)) {
                        matchingEnums.add(enumConstant);
                    }
                }
            }
            if (operator.equals("!=") || operator.equals("=out=")) {
                if (matchingEnums.isEmpty()) {
                    return cb.conjunction();
                }
                return cb.not(path.in(matchingEnums));
            } else {
                if (matchingEnums.isEmpty()) {
                    return cb.disjunction();
                }
                return path.in(matchingEnums);
            }
        }

        if (meta.type == Boolean.class || meta.type == boolean.class) {
            List<Boolean> matchingBools = new ArrayList<>();
            String lowerPattern = cleanVal.replace("*", "").replace("%", "").toLowerCase();
            if ("true".contains(lowerPattern)) {
                matchingBools.add(true);
            }
            if ("false".contains(lowerPattern)) {
                matchingBools.add(false);
            }
            if (operator.equals("!=") || operator.equals("=out=")) {
                if (matchingBools.isEmpty()) {
                    return cb.conjunction();
                }
                return cb.not(path.in(matchingBools));
            } else {
                if (matchingBools.isEmpty()) {
                    return cb.disjunction();
                }
                return path.in(matchingBools);
            }
        }

        if (isNumericType(meta.type)) {
            String clean = cleanVal.replace("*", "").replace("%", "");
            BigDecimal val;
            try {
                val = new BigDecimal(clean);
            } catch (NumberFormatException e) {
                if (operator.equals("!=") || operator.equals("=out=")) {
                    return cb.conjunction();
                } else {
                    return cb.disjunction();
                }
            }

            @SuppressWarnings("unchecked")
            var numExpr = (Expression<? extends Number>) path;
            switch (operator) {
                case "==":
                    return cb.equal(numExpr, val);
                case "!=":
                    return cb.notEqual(numExpr, val);
                case "=gt=":
                    return cb.gt(numExpr, val);
                case "=ge=":
                    return cb.ge(numExpr, val);
                case "=lt=":
                    return cb.lt(numExpr, val);
                case "=le=":
                    return cb.le(numExpr, val);
                default:
                    return cb.equal(numExpr, val);
            }
        }

        if (meta.type == java.util.UUID.class) {
            String clean = cleanVal.replace("*", "").replace("%", "");
            java.util.UUID val;
            try {
                val = java.util.UUID.fromString(clean);
            } catch (IllegalArgumentException e) {
                if (operator.equals("!=") || operator.equals("=out=")) {
                    return cb.conjunction();
                } else {
                    return cb.disjunction();
                }
            }
            if (operator.equals("!=") || operator.equals("=out=")) {
                return cb.notEqual(path, val);
            } else {
                return cb.equal(path, val);
            }
        }

        if (Date.class.isAssignableFrom(meta.type) ||
                java.time.temporal.TemporalAccessor.class.isAssignableFrom(meta.type) ||
                java.util.Calendar.class.isAssignableFrom(meta.type)) {
            java.time.LocalDate localDate = parseLocalDateWithGtUtils(cleanVal);
            java.time.LocalDateTime localDateTime = parseLocalDateTimeWithGtUtils(cleanVal);
            java.util.Date utilDate = parseDateWithGtUtils(cleanVal);

            if (meta.type == java.time.LocalDate.class) {
                java.time.LocalDate val = localDate != null ? localDate
                        : (localDateTime != null ? localDateTime.toLocalDate() : null);
                if (val == null) {
                    return (operator.equals("!=") || operator.equals("=out=")) ? cb.conjunction() : cb.disjunction();
                }
                @SuppressWarnings("unchecked")
                var expr = (Expression<java.time.LocalDate>) path;
                return buildComparablePredicate(expr, cb, operator, val);
            } else if (meta.type == java.time.LocalDateTime.class) {
                java.time.LocalDateTime val = localDateTime != null ? localDateTime
                        : (localDate != null ? localDate.atStartOfDay() : null);
                if (val == null) {
                    return (operator.equals("!=") || operator.equals("=out=")) ? cb.conjunction() : cb.disjunction();
                }
                @SuppressWarnings("unchecked")
                var expr = (Expression<java.time.LocalDateTime>) path;
                return buildComparablePredicate(expr, cb, operator, val);
            } else if (Date.class.isAssignableFrom(meta.type)) {
                java.util.Date val = utilDate;
                if (val == null) {
                    return (operator.equals("!=") || operator.equals("=out=")) ? cb.conjunction() : cb.disjunction();
                }
                @SuppressWarnings("unchecked")
                var expr = (Expression<java.util.Date>) path;
                return buildComparablePredicate(expr, cb, operator, val);
            }
        }

        // Strings and anything else (Default)
        @SuppressWarnings("unchecked")
        var castedPath = (Expression<String>) path;
        Expression<String> stringExpr = cb.lower(castedPath);
        String pattern = cleanVal.replace("*", "%").toLowerCase();
        boolean hasWildcard = cleanVal.contains("*") || cleanVal.contains("%") || operator.equals("=like=") || operator.equals("=ilike=");

        if (operator.equals("==") || operator.equals("=like=") || operator.equals("=ilike=")) {
            if (hasWildcard) {
                if (!pattern.startsWith("%") && !pattern.endsWith("%")) {
                    pattern = "%" + pattern + "%";
                }
                return cb.like(stringExpr, pattern);
            } else {
                return cb.equal(stringExpr, pattern);
            }
        } else if (operator.equals("!=")) {
            if (hasWildcard) {
                if (!pattern.startsWith("%") && !pattern.endsWith("%")) {
                    pattern = "%" + pattern + "%";
                }
                return cb.notLike(stringExpr, pattern);
            } else {
                return cb.notEqual(stringExpr, pattern);
            }
        } else if (operator.equals("=in=")) {
            String[] parts = cleanVal.split(",");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                list.add(p.trim().toLowerCase());
            }
            return stringExpr.in(list);
        } else if (operator.equals("=out=")) {
            String[] parts = cleanVal.split(",");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                list.add(p.trim().toLowerCase());
            }
            return cb.not(stringExpr.in(list));
        }

        return cb.equal(stringExpr, pattern);
    }

    private static <Y extends Comparable<? super Y>> Predicate buildComparablePredicate(Expression<Y> expr,
            CriteriaBuilder cb, String operator, Y val) {
        switch (operator) {
            case "==":
                return cb.equal(expr, val);
            case "!=":
                return cb.notEqual(expr, val);
            case "=gt=":
                return cb.greaterThan(expr, val);
            case "=ge=":
                return cb.greaterThanOrEqualTo(expr, val);
            case "=lt=":
                return cb.lessThan(expr, val);
            case "=le=":
                return cb.lessThanOrEqualTo(expr, val);
            default:
                return cb.equal(expr, val);
        }
    }

    private static FieldMeta getFieldMeta(Class<?> clazz, String path) {
        if (clazz == null || path == null || path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Class<?> current = clazz;
        boolean isCol = false;
        boolean isMap = false;
        Class<?> finalType = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            FieldMeta meta = getDirectFieldMeta(current, part);
            if (meta == null) {
                return null;
            }
            current = meta.type;
            finalType = meta.type;
            if (meta.isCollection) {
                isCol = true;
            }
            if (meta.isMap) {
                isMap = true;
            }
        }
        return new FieldMeta(finalType, isCol, isMap);
    }

    private static FieldMeta getDirectFieldMeta(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                Class<?> fieldType = field.getType();
                boolean isCol = Collection.class.isAssignableFrom(fieldType);
                boolean isMap = Map.class.isAssignableFrom(fieldType);

                Class<?> type = fieldType;
                if (isCol) {
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                        if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                            type = (Class<?>) actualTypeArguments[0];
                        }
                    }
                } else if (isMap) {
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                        if (actualTypeArguments.length > 1 && actualTypeArguments[1] instanceof Class) {
                            type = (Class<?>) actualTypeArguments[1];
                        }
                    }
                }
                return new FieldMeta(type, isCol, isMap);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static java.time.LocalDate parseLocalDateWithGtUtils(String value) {
        for (java.time.format.DateTimeFormatter dtf : com.gt.toolbox.spb.webapps.commons.infra.utils.GtUtils.LOCAL_DATE_FORMATS) {
            try {
                return java.time.LocalDate.parse(value, dtf);
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
    }

    private static java.time.LocalDateTime parseLocalDateTimeWithGtUtils(String value) {
        for (java.time.format.DateTimeFormatter dtf : com.gt.toolbox.spb.webapps.commons.infra.utils.GtUtils.LOCAL_DATE_TIME_FORMATS) {
            try {
                return java.time.LocalDateTime.parse(value, dtf);
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
    }

    private static java.util.Date parseDateWithGtUtils(String value) {
        for (java.text.SimpleDateFormat sdf : com.gt.toolbox.spb.webapps.commons.infra.utils.GtUtils.DATE_FORMATS) {
            try {
                return sdf.parse(value);
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
    }
}
