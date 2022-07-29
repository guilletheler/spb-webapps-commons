package com.gt.toolbox.spb.webapps.commons.infra.service;

//import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.gt.toolbox.spb.webapps.commons.infra.utils.Utils;

public class QueryHelper {

	public static <T> Specification<T> getFilterSpecification(Map<String, String> filterValues) {
		return getFilterSpecification(filterValues, true);
	}

	public static <T> Specification<T> getFilterSpecification(Map<String, String> filterValues,
			boolean concatUsingAnd) {

		return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			// query.distinct(true);
			return buildPredicate(filterValues, root, builder, concatUsingAnd);
		};
	}

	public static <T> Predicate buildPredicate(Map<String, String> filterValues, Root<T> root,
			CriteriaBuilder builder) {
		return buildPredicate(filterValues, root, builder, true);
	}

	public static <T> Predicate buildPredicate(Map<String, String> filterValues, Root<T> root,
			CriteriaBuilder builder, boolean concatUsingAnd) {

		Stream<Predicate> predicates = filterValues.entrySet().stream()
				.filter(v -> v.getValue() != null && v.getValue().length() > 0).map(entry -> {
					Predicate tmp = buildPredicate(root, builder, entry.getKey(), entry.getValue());
					return tmp;
				});

		Optional<Predicate> predicate;

		if (concatUsingAnd) {
			predicate = predicates.collect(Collectors.reducing((a, b) -> builder.and(a, b)));
		} else {
			predicate = predicates.collect(Collectors.reducing((a, b) -> builder.or(a, b)));
		}

		return predicate.orElseGet(() -> alwaysTrue(builder));
	}

	public static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder builder, String originalKey,
			String value) {
		// lo meto dentro de un arreglo para que quede final y se pueda usar dentro de
		// las expresiones lambda
		Path<?> path[] = new Path[] { root };

		String[] splitKey = originalKey.split("\\.");

		for (int i = 0; i < splitKey.length; i++) {
			if (i == 0 && splitKey.length > 1) {
				path[0] = root.join(splitKey[i], JoinType.LEFT);
			} else {
				path[0] = path[0].get(splitKey[i]);
			}
		}

		Predicate ret = buildIntegerPredicate(builder, path[0], value)
				.orElseGet(() -> buildDecimalPredicate(builder, path[0], value)
						.orElseGet(() -> buildBooleanPredicate(builder, path[0], value)
								.orElseGet(() -> buildDatePredicate(builder, path[0], value)
										.orElseGet(() -> buildDefaultPredicate(builder, path[0], value)))));
		return ret;
	}

	public static Predicate buildDefaultPredicate(CriteriaBuilder builder, Path<?> path, String value) {

		Predicate ret = null;

		String[] groupValues = value.split("\\+\\+");

		for (String groupValue : groupValues) {
			String[] orValues = groupValue.split("\\|\\|");

			Predicate groupPredicate = null;

			for (String orValue : orValues) {
				// Logger.getLogger(QueryHelper.class.getName()).info("generando OR para " +
				// orValue);

				String[] andValues = orValue.split("\\&\\&");

				Predicate orPredicate = null;

				for (String andValue : andValues) {
					// Logger.getLogger(QueryHelper.class.getName()).info("agregando and para " +
					// andValue);
					Predicate andPredicate;
					if (andValue.length() > 1 && andValue.startsWith("'") && andValue.endsWith("'")) {
						andValue = andValue.substring(1, andValue.length() - 1);

						andPredicate = builder.like(path.as(String.class),
								andValue);
					} else {
						andPredicate = builder.like(builder.upper(path.as(String.class)),
								"%" + andValue.toUpperCase() + "%");
					}

					if (orPredicate == null) {
						orPredicate = andPredicate;
					} else {
						orPredicate = builder.and(orPredicate, andPredicate);
					}
				}
				if (orPredicate != null) {
					if (groupPredicate == null) {
						groupPredicate = orPredicate;
					} else {
						groupPredicate = builder.or(groupPredicate, orPredicate);
					}
				}
			}

			if (groupPredicate != null) {
				if (ret == null) {
					ret = groupPredicate;
				} else {
					ret = builder.and(ret, groupPredicate);
				}
			}
		}

		return ret;

	}

	/**
	 * NO TERMINADO DEBERÍA BUSCAR EL VALOR DENTRO DE UNA LISTA
	 * 
	 * @param builder
	 * @param path
	 * @param key
	 * @param value
	 * @return
	 *         public Optional<Predicate> buildCollectionPredicate(CriteriaBuilder
	 *         builder, Path<?> path, String value) {
	 * 
	 *         Predicate ret = null;
	 * 
	 *         if (Collection.class.isAssignableFrom(path.getJavaType())) {
	 *         Class<?> clazz = ((Class<?>) ((ParameterizedType) path.getJavaType()
	 *         .getGenericSuperclass()).getActualTypeArguments()[0]);
	 * 
	 *         CriteriaQuery<?> query = builder.createQuery(clazz);
	 *         Root<?> collectionRoot = query.from(clazz);
	 * 
	 *         }
	 * 
	 *         return Optional.empty();
	 *         }
	 */

	public static Optional<Predicate> buildDatePredicate(CriteriaBuilder builder, Path<?> path,
			String value) {
		if (Objects.equals(path.getJavaType(), Date.class)) {

			Expression<Date> dateExpression = path.as(Date.class);

			if (value.trim().startsWith("-") || value.trim().startsWith(">")) {
				Date fechaIni = QueryHelper.parseDate(value.trim().substring(1).trim());
				return Optional.ofNullable(builder.greaterThanOrEqualTo(dateExpression, fechaIni));
			} else if (value.trim().startsWith("<")) {
				Date fechaFin = QueryHelper.parseDate(value.trim().substring(1).trim());
				return Optional.ofNullable(builder.lessThanOrEqualTo(dateExpression, fechaFin));
			} else if (value.trim().endsWith("-")) {
				Date fechaFin = QueryHelper.parseDate(value.trim().substring(0, value.trim().length() - 1).trim());
				return Optional.ofNullable(builder.lessThanOrEqualTo(dateExpression, fechaFin));
			} else if (value.trim().contains("-")) {
				// Supongo un between

				String[] fechas = value.trim().split("-");

				if (fechas.length < 2) {
					fechas = new String[] { fechas[0], "01/01/2100" };
				}

				Date fechaIni = QueryHelper.parseDate(fechas[0].trim());
				Date fechaFin = parseDate(fechas[1].trim());

				if (fechaIni == null) {
					fechaIni = QueryHelper.parseDate("01/01/1900");
				}
				if (fechaFin == null) {
					fechaFin = QueryHelper.parseDate("01/01/2100");
				}

				return Optional.ofNullable(builder.between(dateExpression, fechaIni, fechaFin));
				// return null;

			} else {

				return Optional.ofNullable(builder.like(builder.function("TO_CHAR", String.class, dateExpression,
						builder.literal("dd/MM/yyyy HH24:MI:ss")), "%" + value + "%"));

			}
		}

		return Optional.empty();
	}

	public static Optional<Predicate> buildBooleanPredicate(CriteriaBuilder builder, Path<?> path,
			String value) {

		if (Objects.equals(path.getJavaType(), boolean.class)
				|| Objects.equals(path.getJavaType(), Boolean.class)) {
			Boolean valor = value.trim().equalsIgnoreCase("si")
					|| value.trim().equalsIgnoreCase("true");

			if (valor) {
				return Optional.ofNullable(builder.equal(builder.coalesce(path, Boolean.FALSE), Boolean.TRUE));
			}

			valor = value.trim().equalsIgnoreCase("no")
					|| value.trim().equalsIgnoreCase("false");

			if (valor) {
				return Optional.ofNullable(builder.equal(builder.coalesce(path, Boolean.FALSE), Boolean.FALSE));
			}

		}
		return Optional.empty();
	}

	public static Optional<Predicate> buildDecimalPredicate(CriteriaBuilder builder, Path<?> path,
			String value) {

		if (isDecimalClass(path.getJavaType())) {

			// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
			// "Generando predicado de decimal para " + value);

			Expression<BigDecimal> numberExpression = path.as(BigDecimal.class);

			BigDecimal tmpDoubleValue;
			String tmpString = "";

			try {
				if (value.startsWith("0") || value.startsWith("=")) {
					tmpString = value.substring(1).trim().replace(",", ".");
					tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
					return Optional.ofNullable(builder.equal(numberExpression, tmpDoubleValue));
				}
				if (value.startsWith("<")) {
					tmpString = value.substring(1).trim().replace(",", ".");
					tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
					return Optional.ofNullable(builder.le(numberExpression, tmpDoubleValue));
				}
				if (value.startsWith(">")) {
					tmpString = value.substring(1).trim().replace(",", ".");
					tmpDoubleValue = BigDecimal.valueOf(Double.valueOf(tmpString));
					return Optional.ofNullable(builder.ge(numberExpression, tmpDoubleValue));
				}
			} catch (NumberFormatException ex) {
				// Logger.getLogger(QueryHelper.class.getName()).log(Level.WARNING,
				// "No se puede convertir tmpString '" + tmpString + "' a decimal, entrada '" +
				// value + "'");
			}
		}
		return Optional.empty();
	}

	public static Optional<Predicate> buildIntegerPredicate(CriteriaBuilder builder, Path<?> path,
			String value) {

		if (isIntegerClass(path.getJavaType())) {

			Expression<BigInteger> numberExpression = path.as(BigInteger.class);

			// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
			// "Generando predicado de entero para " + value);

			BigInteger tmpLongValue;
			String tmpString = "";

			try {
				if (value.startsWith("0") || value.startsWith("=")) {
					tmpString = value.substring(1).trim();
					tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
					return Optional.ofNullable(builder.equal(numberExpression, tmpLongValue));
				}
				if (value.startsWith("<")) {
					tmpString = value.substring(1).trim();
					tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
					return Optional.ofNullable(builder.le(numberExpression, tmpLongValue));
				}
				if (value.startsWith(">")) {
					tmpString = value.substring(1).trim();
					tmpLongValue = BigInteger.valueOf(Long.valueOf(tmpString));
					return Optional.ofNullable(builder.ge(numberExpression, tmpLongValue));
				}
			} catch (NumberFormatException ex) {
				// Logger.getLogger(QueryHelper.class.getName()).log(Level.WARNING,
				// "No se puede convertir tmpString '" + tmpString + "' a integer, entrada '" +
				// value + "'");
			}
		}

		return Optional.empty();
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

	public static Predicate alwaysTrue(CriteriaBuilder builder) {
		return builder.isTrue(builder.literal(true));
	}

	public static Predicate alwaysFalse(CriteriaBuilder builder) {
		return builder.isTrue(builder.literal(false));
	}

	public static boolean isCollectionClass(Class<?> clazz) {
		return clazz.isAssignableFrom(Collection.class);
	}

	public static boolean isIntegerClass(Class<?> clazz) {
		return Objects.equals(Byte.class, clazz) || Objects.equals(byte.class, clazz)
				|| Objects.equals(Integer.class, clazz) || Objects.equals(int.class, clazz)
				|| Objects.equals(Long.class, clazz) || Objects.equals(long.class, clazz)
				|| Objects.equals(BigInteger.class, clazz);
	}

	public static boolean isDecimalClass(Class<?> clazz) {
		return Objects.equals(Float.class, clazz) || Objects.equals(float.class, clazz)
				|| Objects.equals(Double.class, clazz) || Objects.equals(double.class, clazz)
				|| Objects.equals(BigDecimal.class, clazz);
	}

}
