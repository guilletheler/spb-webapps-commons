package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.*;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SpecificationArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SpecificationArgumentResolver.class);

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
            filter = webRequest.getParameter("query");
        }
        if (filter == null || filter.trim().isEmpty()) {
            filter = webRequest.getParameter("spec");
        }
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

            return new Specification<Object>() {
                @Override
                public Predicate toPredicate(@NonNull Root<Object> root,
                        @Nullable CriteriaQuery<?> query,
                        @NonNull CriteriaBuilder cb) {
                    try {
                        Path<?> path = resolvePath(root, field);
                        Class<?> fieldType = path.getJavaType();
                        boolean isCollection = Collection.class.isAssignableFrom(fieldType);

                        boolean wasQuoted = (value.startsWith("'") && value.endsWith("'"))
                                || (value.startsWith("\"") && value.endsWith("\""));
                        String cleanVal = value;
                        if (wasQuoted) {
                            cleanVal = value.substring(1, value.length() - 1);
                        }

                        Predicate pred = buildPredicate(path, cb, fieldType, operator, cleanVal, wasQuoted,
                                isCollection);

                        return pred;
                    } catch (Exception e) {
                        LOG.error("Error creating predicate for field: " + field, e);
                        return cb.disjunction();
                    }
                }
            };
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

    private static Path<?> resolvePath(Path<?> root, String fieldName) {
        String[] splitKey = fieldName.split("\\.");
        Path<?> path = root;
        Class<?> curClass = root.getJavaType();

        for (int i = 0; i < splitKey.length; i++) {
            String part = splitKey[i];
            Method m = getPathMethod(curClass, part);
            Class<?> returnType = m.getReturnType();

            Path<?> nextPath = null;
            if (path instanceof From<?, ?> from) {
                for (jakarta.persistence.criteria.Join<?, ?> join : from.getJoins()) {
                    if (join.getAttribute().getName().equals(part)) {
                        nextPath = join;
                        break;
                    }
                }
            }

            if (nextPath == null) {
                if (returnType.getAnnotation(Entity.class) != null) {
                    if (path instanceof From<?, ?> from) {
                        nextPath = from.join(part, JoinType.LEFT);
                    } else {
                        nextPath = path.get(part);
                    }
                } else {
                    nextPath = path.get(part);
                }
            }

            path = nextPath;
            curClass = returnType;
            String curPath = String.join("_", java.util.Arrays.copyOfRange(splitKey, 0, i + 1));
            path.alias(curPath);
        }
        return path;
    }

    private static Method getPathMethod(Class<?> curClass, String fieldName) {
        var methodName = "get" + StringUtils.capitalize(fieldName);
        Method m = null;
        try {
            m = curClass.getMethod(methodName);
        } catch (NoSuchMethodException | SecurityException e) {
            methodName = "is" + StringUtils.capitalize(fieldName);
            try {
                m = curClass.getMethod(methodName);
            } catch (NoSuchMethodException | SecurityException ex) {
                LOG.info("Error al acceder al método {} de la clase {}",
                        StringUtils.capitalize(fieldName), curClass);
                throw new RuntimeException("Error en los campos de filtro", e);
            }
        }
        return m;
    }

    private static Predicate buildPredicate(Path<?> path, CriteriaBuilder cb, Class<?> fieldType, String operator,
            String value, boolean wasQuoted, boolean isCollection) {
        Predicate ret = null;
        if (operator.equals("=in=") || operator.equals("=out=")) {
            String valExpr = value;
            if (valExpr.startsWith("(") && valExpr.endsWith(")")) {
                valExpr = valExpr.substring(1, valExpr.length() - 1);
            }
            String[] parts = valExpr.split(",");
            List<Predicate> orPredicates = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                boolean partQuoted = (trimmed.startsWith("'") && trimmed.endsWith("'"))
                        || (trimmed.startsWith("\"") && trimmed.endsWith("\""));
                String partClean = trimmed;
                if (partQuoted) {
                    partClean = trimmed.substring(1, trimmed.length() - 1);
                }
                Predicate single = buildSinglePredicate(path, cb, fieldType, "==", partClean, partQuoted, isCollection);
                if (single != null) {
                    orPredicates.add(single);
                }
            }
            if (!orPredicates.isEmpty()) {
                ret = cb.or(orPredicates.toArray(new Predicate[0]));
            }
        } else {
            ret = buildSinglePredicate(path, cb, fieldType, operator, value, wasQuoted, isCollection);
        }

        if (ret != null && (operator.equals("!=") || operator.equals("=out="))) {
            ret = ret.not();
        }

        return ret;
    }

    private static Predicate buildSinglePredicate(Path<?> path, CriteriaBuilder cb, Class<?> fieldType, String operator,
            String cleanVal, boolean wasQuoted, boolean isCollection) {
        String mappedExpr = mapSingleValue(fieldType, operator, cleanVal, wasQuoted);

        if (isCollection) {
            return CollectionPredicateBuilder.buildPredicate(cb, path, mappedExpr);
        }

        Predicate predicate = null;
        if (mappedExpr != null && !mappedExpr.isBlank()) {
            boolean replacePredicate = true;
            if (IntegerPredicateBuilder.isIntegerClass(fieldType)) {
                replacePredicate = false;
                predicate = IntegerPredicateBuilder.buildPredicate(cb, path, mappedExpr);
            } else if (DecimalPredicateBuilder.isDecimalClass(fieldType)) {
                replacePredicate = false;
                predicate = DecimalPredicateBuilder.buildPredicate(cb, path, mappedExpr);
            } else if (BooleanPredicateBuilder.isBooleanClass(fieldType)) {
                replacePredicate = false;
                predicate = BooleanPredicateBuilder.buildPredicate(cb, path, mappedExpr);
            } else if (DatePredicateBuilder.isDateClass(fieldType)) {
                replacePredicate = false;
                predicate = DatePredicateBuilder.buildPredicate(cb, path, mappedExpr);
            } else if (TimePredicateBuilder.isTimeClass(fieldType)) {
                replacePredicate = false;
                predicate = TimePredicateBuilder.buildPredicate(cb, path, mappedExpr);
            }

            if (predicate == null && replacePredicate) {
                predicate = StringPredicateBuilder.buildPredicate(cb, path, mappedExpr, operator);
            }
        }
        return predicate;
    }

    private static String mapSingleValue(Class<?> type, String operator, String cleanVal, boolean wasQuoted) {
        if (BooleanPredicateBuilder.isBooleanClass(type)) {
            return cleanVal;
        }

        if (IntegerPredicateBuilder.isIntegerClass(type)
                || DecimalPredicateBuilder.isDecimalClass(type)
                || DatePredicateBuilder.isDateClass(type)
                || TimePredicateBuilder.isTimeClass(type)) {
            if (operator.equals("=like=") || operator.equals("=ilike=")) {
                return cleanVal;
            }
            switch (operator) {
                case "==":
                case "!=":
                    return "=" + cleanVal;
                case "=gt=":
                    return ">" + cleanVal;
                case "=ge=":
                    return ">=" + cleanVal;
                case "=lt=":
                    return "<" + cleanVal;
                case "=le=":
                    return "<=" + cleanVal;
                default:
                    return "=" + cleanVal;
            }
        }

        if (operator.equals("==") || operator.equals("=like=") || operator.equals("=ilike=")) {
            if (cleanVal.contains("*")) {
                return "'" + cleanVal.replace('*', '%') + "'";
            }
            if (operator.equals("=ilike=")) {
                return cleanVal;
            }
            if (wasQuoted) {
                return "'" + cleanVal + "'";
            }
            return cleanVal;
        }

        return cleanVal;
    }
}
