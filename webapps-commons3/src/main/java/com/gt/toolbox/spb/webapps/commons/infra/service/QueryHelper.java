package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.lang.reflect.Method;
//import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import com.gt.toolbox.spb.webapps.commons.infra.utils.Utils;
import com.gt.toolbox.spb.webapps.payload.FilterMeta;

import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;


public class QueryHelper {

	public static <T> Specification<T> getFilterSpecification(FilterMeta filter) {

		return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			// query.distinct(true);

			return buildPredicate(root, builder, filter);
		};
	}

	public static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder builder, FilterMeta filter) {
		return buildPredicate(root, builder, filter, new ArrayList<>());
	}

	public static <T> Predicate buildPredicate(Path<?> path, CriteriaBuilder builder, FilterMeta filter) {
		return buildPredicate(path, builder, filter, new ArrayList<>());
	}

	private static <T> Predicate buildPredicateAgrupador(Path<?> path, CriteriaBuilder builder, FilterMeta filter,
			List<String> pathAgregados) {

		Predicate ret = null;

		for (var child : filter.getChildrens()) {
			Predicate tmpPredicate = buildPredicate(path, builder, child, pathAgregados);
			if (tmpPredicate != null) {

				if (ret == null) {
					ret = tmpPredicate;
				} else {
					switch (filter.getOperator().toUpperCase()) {
						case "AND":
							ret = builder.and(ret, tmpPredicate);
							break;
						case "OR":
							ret = builder.or(ret, tmpPredicate);
							break;
						default:
							throw new IllegalArgumentException("Operador incorrecto");
					}
				}
			}
		}

		return ret;
	}

	private static <T> Predicate buildPredicate(Path<?> path, CriteriaBuilder builder, FilterMeta filter,
			List<String> pathAgregados) {

		Predicate ret = null;

		if (filter.getChildrens() != null && !filter.getChildrens().isEmpty()) {
			ret = buildPredicateAgrupador(path, builder, filter, pathAgregados);
		} else {
			// valor directo

			if (filter.getFieldName() != null && !filter.getFieldName().isEmpty()) {
				while (path.getParentPath() != null) {
					path = path.getParentPath();
				}

				String[] splitKey = filter.getFieldName().split("\\.");

				// if (splitKey.length == 1) {
				// path = path.get(splitKey[0]);
				// path.alias(splitKey[0].replace(".", "_"));
				// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO, "Seteando path
				// de alias " + path.getAlias());
				// } else {
				String curPath = "";

				// Esto no anda con las colecciones
				Class<?> curClass = path.getJavaType();

				for (int i = 0; i < splitKey.length; i++) {
					if (!curPath.isEmpty()) {
						curPath += ".";
					}
					curPath += splitKey[i];

					Method m = getPathMethod(curClass, splitKey[i]);

					if (!pathAgregados.contains(curPath)) {

						if (m.getReturnType().getAnnotation(Entity.class) != null) {

							path = agregarJoin(path, pathAgregados, curPath, splitKey[i]);
						} else {
							// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
							// "siguiendo path " + splitKey[i] + " " + m.getName() + " " +
							// m.getReturnType());
							path = path.get(splitKey[i]);
						}

					} else {
						if (Root.class.isAssignableFrom(path.getClass())) {
							for (var join : ((Root<?>) path).getJoins()) {
								if (join.getAttribute().getName().equals(splitKey[i])) {
									path = join;
									break;
								}
							}

						} else {
							Logger.getLogger(QueryHelper.class.getName()).log(Level.WARNING,
									"GUARDA QUE NO ES ROOT!! Siguiendo path join a " + curPath + " "
											+ path.getJavaType());
							path = path.get(splitKey[i]);

						}
					}

					curClass = m.getReturnType();
					path.alias(curPath.replace(".", "_"));
					// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO, "Seteando path
					// de alias " + path.getAlias());
				}
				// }

				ret = buildPredicate(path, builder, filter.getValue());

			}
		}

		if (ret != null && filter.getOperator() != null && filter.getOperator().equalsIgnoreCase("NOT")) {
			ret = ret.not();
		}

		return ret;
	}

	private static Path<?> agregarJoin(Path<?> path, List<String> pathAgregados, String curPath,
			String curKey) {

		pathAgregados.add(curPath);

		if (Root.class.isAssignableFrom(path.getClass())) {
			path = ((Root<?>) path).join(curKey, JoinType.LEFT);
		} else {
			path = ((Join<?, ?>) path).join(curKey, JoinType.LEFT);
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
				// No hace nada, o no es un método o no puede acceder
				Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
						"Error al acceder al método " + StringUtils.capitalize(fieldName)
								+ " de la clase " + curClass);
				throw new RuntimeException("Error en los campos de filtro", e);
			}
		}
		return m;
	}

	public static <T> Predicate buildPredicate(Path<?> path, CriteriaBuilder builder, String value) {

		Predicate ret = buildCollectionPredicate(builder, path, value)
				.orElseGet(() -> buildIntegerPredicate(builder, path, value)
						.orElseGet(() -> buildDecimalPredicate(builder, path, value)
								.orElseGet(() -> buildBooleanPredicate(builder, path, value)
										.orElseGet(() -> buildDatePredicate(builder, path, value)
												.orElseGet(() -> buildDefaultPredicate(builder, path, value))))));

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
	 * No funciona bien, trae resultados repetidos
	 * 
	 * @param builder
	 * @param path
	 * @param value
	 * @return
	 */
	public static Optional<Predicate> buildCollectionPredicate(CriteriaBuilder builder, Path<?> path, String value) {

		if (Collection.class.isAssignableFrom(path.getJavaType())) {

			var collectionPropertyName = path.getAlias();

			Path<?> parentPath = path.getParentPath();

			if (Root.class.isAssignableFrom(parentPath.getClass())) {
				path = ((Root<?>) parentPath).join(collectionPropertyName, JoinType.LEFT);
			} else {
				path = ((Join<?, ?>) parentPath).join(collectionPropertyName, JoinType.LEFT);
			}

			// while(tmpPath != null) {
			// strPath = tmpPath.getJavaType().getSimpleName() + "." + strPath;
			// tmpPath = tmpPath.getParentPath();
			// }

			Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
					"Creando predicate para collection " + value + " in " + collectionPropertyName);

			var predicate = builder.like(path.as(String.class), "%" + value + "%");

			return Optional.of(predicate);

		}

		return Optional.empty();
	}

	/**
	 * 
	 * @param builder
	 * @param path
	 * @param key
	 * @param value
	 * @return
	 */
	public static Optional<Predicate> buildDatePredicate(CriteriaBuilder builder, Path<?> path,
			String value) {
		if (Objects.equals(path.getJavaType(), Date.class)) {

			Expression<Date> dateExpression = path.as(Date.class);

			if (value.trim().startsWith("-") || value.trim().startsWith(">")) {
				Date fechaFin = QueryHelper.parseDate(value.trim().substring(1).trim());
				return Optional.ofNullable(builder.lessThanOrEqualTo(dateExpression, fechaFin));
			} else if (value.trim().startsWith("<")) {
				Date fechaFin = QueryHelper.parseDate(value.trim().substring(1).trim());
				return Optional.ofNullable(builder.lessThanOrEqualTo(dateExpression, fechaFin));
			} else if (value.trim().endsWith("-")) {
				Date fechaIni = QueryHelper.parseDate(value.trim().substring(0, value.trim().length() - 1).trim());
				return Optional.ofNullable(builder.greaterThanOrEqualTo(dateExpression, fechaIni));
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

				return Optional.ofNullable(builder.equal(dateExpression, QueryHelper.parseDate(value)));

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