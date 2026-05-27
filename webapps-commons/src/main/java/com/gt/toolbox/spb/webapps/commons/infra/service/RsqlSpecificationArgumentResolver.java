package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
/**
 * Resolver de argumentos de controlador que mapea parámetros de consulta "filter" en formato RSQL
 * a objetos {@link Specification} de Spring Data JPA.
 *
 * <p>Para configurarlo en una aplicación Spring Boot, agregue el resolver a la configuración de Spring WebMvc:</p>
 * <pre>{@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *
 *     @Override
 *     public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
 *         resolvers.add(new RsqlSpecificationArgumentResolver());
 *     }
 * }
 * }</pre>
 */
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

    private static class CastedCondition {
        final String field;
        final String operator;
        final String value;
        final FieldMeta meta;

        CastedCondition(String field, String operator, String value, FieldMeta meta) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.meta = meta;
        }
    }

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
            return RSQLJPASupport.toSpecification(filter);
        }

        final String originalFilter = filter;
        final List<CastedCondition> castedConditions = new ArrayList<>();
        final String cleanedFilter = cleanFilter(filter, entityClass, castedConditions);

        return new Specification<Object>() {
            @Override
            public Predicate toPredicate(@NonNull Root<Object> root,
                    @Nullable CriteriaQuery<?> query,
                    @NonNull CriteriaBuilder cb) {
                Predicate rsqlPredicate = null;
                if (cleanedFilter != null && !cleanedFilter.trim().isEmpty()) {
                    Specification<Object> rsqlSpec = RSQLJPASupport.toSpecification(cleanedFilter);
                    rsqlPredicate = rsqlSpec.toPredicate(root, query, cb);
                }

                List<Predicate> customPredicates = new ArrayList<>();
                for (CastedCondition cond : castedConditions) {
                    try {
                        Expression<?> path = root;
                        String[] parts = cond.field.split("\\.");
                        for (int i = 0; i < parts.length; i++) {
                            String part = parts[i];
                            if (i == parts.length - 1) {
                                if (cond.meta.isCollection) {
                                    path = ((From<?, ?>) path).join(part, JoinType.LEFT);
                                } else if (cond.meta.isMap) {
                                    path = ((From<?, ?>) path).joinMap(part, JoinType.LEFT).value();
                                } else {
                                    path = ((Path<?>) path).get(part);
                                }
                            } else {
                                path = ((From<?, ?>) path).join(part, JoinType.LEFT);
                            }
                        }
                        Expression<String> stringExpr;
                        if (Date.class.isAssignableFrom(cond.meta.type) ||
                                java.util.Calendar.class.isAssignableFrom(cond.meta.type) ||
                                java.time.temporal.TemporalAccessor.class.isAssignableFrom(cond.meta.type)) {
                            stringExpr = cb.lower(cb.function("to_char", String.class, path, cb.literal("DD/MM/YYYY HH24:MI:SS")));
                        } else {
                            stringExpr = cb.lower(path.cast(String.class));
                        }
                        String cleanVal = cond.value;
                        if (cleanVal.startsWith("'") && cleanVal.endsWith("'")) {
                            cleanVal = cleanVal.substring(1, cleanVal.length() - 1);
                        } else if (cleanVal.startsWith("\"") && cleanVal.endsWith("\"")) {
                            cleanVal = cleanVal.substring(1, cleanVal.length() - 1);
                        }
                        String pattern = cleanVal.replace("*", "%").toLowerCase();
                        if (cond.operator.equals("!=") || cond.operator.equals("=out=")) {
                            customPredicates.add(cb.notLike(stringExpr, pattern));
                        } else {
                            customPredicates.add(cb.like(stringExpr, pattern));
                        }
                    } catch (Exception e) {
                        // Ignore path traversal errors
                    }
                }

                boolean useAnd = originalFilter.contains(";") && !originalFilter.contains(",");

                if (rsqlPredicate == null) {
                    if (customPredicates.isEmpty()) {
                        return null;
                    }
                    return useAnd ? cb.and(customPredicates.toArray(new Predicate[0]))
                            : cb.or(customPredicates.toArray(new Predicate[0]));
                } else {
                    if (customPredicates.isEmpty()) {
                        return rsqlPredicate;
                    }
                    List<Predicate> all = new ArrayList<>();
                    all.add(rsqlPredicate);
                    all.addAll(customPredicates);
                    return useAnd ? cb.and(all.toArray(new Predicate[0]))
                            : cb.or(all.toArray(new Predicate[0]));
                }
            }
        };
    }

    private static String cleanFilter(String filter, Class<?> entityClass, List<CastedCondition> castedConditions) {
        Pattern pattern = Pattern.compile(
                "([a-zA-Z0-9_.]+)(==|=like=|!=|=gt=|=lt=|=ge=|=le=|=in=|=out=)('[^']*'|\\\"[^\\\"]*\\\"|\\([^)]*\\)|[^\\s,;()]+)");
        Matcher matcher = pattern.matcher(filter);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(filter, lastEnd, matcher.start());
            String field = matcher.group(1);
            String operator = matcher.group(2);
            String value = matcher.group(3);

            FieldMeta meta = getFieldMeta(entityClass, field);
            if (meta != null && (meta.isCollection || meta.isMap)) {
                if (operator.equals("==") || operator.equals("!=") || operator.equals("=like=")) {
                    if (isCastableToString(meta.type)) {
                        castedConditions.add(new CastedCondition(field, operator, value, meta));
                    }
                }
            } else if (meta != null && isCompatible(meta.type, value)) {
                if (meta.type == String.class) {
                    String newOperator = operator;
                    if (operator.equals("==") || operator.equals("=like=")) {
                        newOperator = "=ilike=";
                    } else if (operator.equals("!=") || operator.equals("=notlike=")) {
                        newOperator = "=inotlike=";
                    }
                    sb.append(field).append(newOperator).append(value);
                } else {
                    sb.append(matcher.group(0));
                }
            } else if (meta != null) {
                if (operator.equals("==") || operator.equals("!=") || operator.equals("=like=")) {
                    if (isCastableToString(meta.type)) {
                        castedConditions.add(new CastedCondition(field, operator, value, meta));
                    }
                }
            } else {
                // Unknown field, keep it to be safe
                sb.append(matcher.group(0));
            }
            lastEnd = matcher.end();
        }
        sb.append(filter, lastEnd, filter.length());

        String cleaned = sb.toString();

        String prev;
        do {
            prev = cleaned;
            cleaned = cleaned.replace(",,", ",")
                    .replace(";;", ";")
                    .replace(",;", ";")
                    .replace(";,", ";")
                    .replace("(,", "(")
                    .replace("(;", "(")
                    .replace(",)", ")")
                    .replace(";)", ")")
                    .replace("()", "");
        } while (!cleaned.equals(prev));

        cleaned = cleaned.trim();
        if (cleaned.startsWith(","))
            cleaned = cleaned.substring(1);
        if (cleaned.startsWith(";"))
            cleaned = cleaned.substring(1);
        if (cleaned.endsWith(","))
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        if (cleaned.endsWith(";"))
            cleaned = cleaned.substring(0, cleaned.length() - 1);

        return cleaned;
    }

    private static boolean isCastableToString(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type == String.class || type == java.util.UUID.class || type.isEnum()) {
            return true;
        }
        if (type.isPrimitive()) {
            return type != void.class;
        }
        if (Number.class.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type) ||
                Character.class.isAssignableFrom(type)) {
            return true;
        }
        if (Date.class.isAssignableFrom(type) ||
                java.util.Calendar.class.isAssignableFrom(type) ||
                java.time.temporal.TemporalAccessor.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
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

    private static boolean isCompatible(Class<?> type, String value) {
        if (type == null) {
            return true;
        }
        String cleanVal = value;
        if (cleanVal.startsWith("'") && cleanVal.endsWith("'")) {
            cleanVal = cleanVal.substring(1, cleanVal.length() - 1);
        } else if (cleanVal.startsWith("\"") && cleanVal.endsWith("\"")) {
            cleanVal = cleanVal.substring(1, cleanVal.length() - 1);
        }
        if (cleanVal.startsWith("*")) {
            cleanVal = cleanVal.substring(1);
        }
        if (cleanVal.endsWith("*")) {
            cleanVal = cleanVal.substring(0, cleanVal.length() - 1);
        }

        if (type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class ||
                type == Short.class || type == short.class ||
                type == BigDecimal.class || type == BigInteger.class) {
            if (value.contains("*")) {
                return false;
            }
            try {
                new BigDecimal(cleanVal);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (type == Boolean.class || type == boolean.class) {
            if (value.contains("*")) {
                return false;
            }
            return "true".equalsIgnoreCase(cleanVal) || "false".equalsIgnoreCase(cleanVal);
        }

        if (Date.class.isAssignableFrom(type) ||
                java.time.temporal.TemporalAccessor.class.isAssignableFrom(type)) {
            if (value.contains("*")) {
                return false;
            }
            return cleanVal.matches("^[0-9T Z:.-]+$");
        }

        if (type == java.util.UUID.class) {
            if (value.contains("*")) {
                return false;
            }
            try {
                java.util.UUID.fromString(cleanVal);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        if (type.isEnum()) {
            if (value.contains("*")) {
                return false;
            }
            for (Object enumConstant : type.getEnumConstants()) {
                if (((Enum<?>) enumConstant).name().equals(cleanVal)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}
