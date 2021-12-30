package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.gt.toolbox.spb.webapps.commons.infra.utils.Utils;

public class QueryHelper {

	public static <T> Specification<T> getFilterSpecification(Map<String, String> filterValues) {

		return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			return buildPredicate(filterValues, root, builder);
		};
	}
	
	public static <T> Predicate buildPredicate(Map<String, String> filterValues, Root<T> root,
			CriteriaBuilder builder) {
		Optional<Predicate> predicate = filterValues.entrySet().stream()
				.filter(v -> v.getValue() != null && v.getValue().length() > 0).map(entry -> {
					Path<?> path = root;
					String key = entry.getKey();

					while (key.contains(".")) {
						String[] splitKey = key.split("\\.");
						path = path.get(splitKey[0]);
						key = key.substring(key.indexOf('.') + 1);
					}

//						Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO, "Generando predicado para clase "
//								+ path.get(key).getJavaType().getName() + " con valor " + entry.getValue());

					final Path<?> finalPath = path;
					final String finalKey = key;

					Predicate tmp = buildIntegerPredicate(builder, finalPath, finalKey, entry.getValue())
							.orElseGet(() -> buildDecimalPredicate(builder, finalPath, finalKey, entry.getValue())
									.orElseGet(
											() -> buildBooleanPredicate(builder, finalPath, finalKey, entry.getValue())
													.orElseGet(() -> buildDatePredicate(builder, finalPath, finalKey,
															entry.getValue())
																	.orElseGet(() -> buildDefaultPredicate(builder,
																			finalPath, finalKey, entry.getValue())))));

					return tmp;

				}).collect(Collectors.reducing((a, b) -> builder.and(a, b)));

		return predicate.orElseGet(() -> alwaysTrue(builder));
	}

	public static Predicate buildDefaultPredicate(CriteriaBuilder builder, Path<?> path, String key, String value) {

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

						andPredicate = builder.like(path.get(key).as(String.class),
								andValue);
					} else {
						andPredicate = builder.like(builder.upper(path.get(key).as(String.class)),
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
	 */
	public Predicate buildCollectionPredicate(CriteriaBuilder builder, Path<?> path, String key, String value) {
		Predicate ret = null;

		String[] values = value.split("%or%");

		for (String val : values) {
			Predicate part = builder.like(builder.upper(path.get(key).as(String.class)), "%" + val.toUpperCase() + "%");
			if (ret == null) {
				ret = part;
			} else {
				ret = builder.or(ret, part);
			}
		}

		return ret;
	}

	public static Optional<Predicate> buildDatePredicate(CriteriaBuilder builder, Path<?> path, String key,
			String value) {
		if (Objects.equals(path.get(key).getJavaType(), Date.class)) {
			if (value.trim().startsWith("-") || value.trim().startsWith(">")) {
				Date fechaIni = QueryHelper.parseDate(value.trim().substring(1).trim());
				return Optional.of(builder.greaterThanOrEqualTo(path.get(key), fechaIni));
			} else if (value.trim().startsWith("<")) {
				Date fechaFin = QueryHelper.parseDate(value.trim().substring(1).trim());
				return Optional.of(builder.lessThanOrEqualTo(path.get(key), fechaFin));
			} else if (value.trim().endsWith("-")) {
				Date fechaFin = QueryHelper.parseDate(value.trim().substring(0, value.trim().length() - 1).trim());
				return Optional.of(builder.lessThanOrEqualTo(path.get(key), fechaFin));
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

				return Optional.of(builder.between(path.get(key), fechaIni, fechaFin));
				// return null;

			} else {

				return Optional.of(builder.like(builder.function("TO_CHAR", String.class, path.get(key),
						builder.literal("dd/MM/yyyy HH24:MI:ss")), "%" + value + "%"));

			}
		}

		return Optional.empty();
	}

	public static Optional<Predicate> buildBooleanPredicate(CriteriaBuilder builder, Path<?> path, String key,
			String value) {

		if (Objects.equals(path.get(key).getJavaType(), boolean.class)
				|| Objects.equals(path.get(key).getJavaType(), Boolean.class)) {
			Boolean valor = value.trim().equalsIgnoreCase("s") || value.trim().equalsIgnoreCase("si")
					|| value.trim().equalsIgnoreCase("t") || value.trim().equalsIgnoreCase("true");
			return Optional.of(builder.equal(path.get(key), valor));
		}
		return Optional.empty();
	}

	public static Optional<Predicate> buildDecimalPredicate(CriteriaBuilder builder, Path<?> path, String key,
			String value) {

		if (isDecimalClass(path.get(key).getJavaType())) {

			// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
			// "Generando predicado de decimal para " + value);

			Double tmpDoubleValue;
			String tmpString = "";

			try {
				if (value.startsWith("0") || value.startsWith("=")) {
					tmpString = value.substring(1).trim().replace(",", ".");
					tmpDoubleValue = Double.valueOf(tmpString);
					return Optional.of(builder.equal(path.get(key), tmpDoubleValue));
				}
				if (value.startsWith("<")) {
					tmpString = value.substring(1).trim().replace(",", ".");
					tmpDoubleValue = Double.valueOf(tmpString);
					return Optional.of(builder.le(path.get(key), tmpDoubleValue));
				}
				if (value.startsWith(">")) {
					tmpString = value.substring(1).trim().replace(",", ".");
					tmpDoubleValue = Double.valueOf(tmpString);
					return Optional.of(builder.ge(path.get(key), tmpDoubleValue));
				}
			} catch (NumberFormatException ex) {
				Logger.getLogger(QueryHelper.class.getName()).log(Level.WARNING,
						"No se puede convertir tmpString '" + tmpString + "' a decimal, entrada '" + value + "'");
			}
		}
		return Optional.empty();
	}

	public static Optional<Predicate> buildIntegerPredicate(CriteriaBuilder builder, Path<?> path, String key,
			String value) {

		if (isIntegerClass(path.get(key).getJavaType())) {

			// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
			// "Generando predicado de entero para " + value);

			Long tmpLongValue;
			String tmpString = "";

			try {
				if (value.startsWith("0") || value.startsWith("=")) {
					tmpString = value.substring(1).trim();
					tmpLongValue = Long.valueOf(tmpString);
					return Optional.of(builder.equal(path.get(key), tmpLongValue));
				}
				if (value.startsWith("<")) {
					tmpString = value.substring(1).trim();
					tmpLongValue = Long.valueOf(tmpString);
					return Optional.of(builder.le(path.get(key), tmpLongValue));
				}
				if (value.startsWith(">")) {
					tmpString = value.substring(1).trim();
					tmpLongValue = Long.valueOf(tmpString);
					return Optional.of(builder.ge(path.get(key), tmpLongValue));
				}
			} catch (NumberFormatException ex) {
				Logger.getLogger(QueryHelper.class.getName()).log(Level.WARNING,
						"No se puede convertir tmpString '" + tmpString + "' a integer, entrada '" + value + "'");
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
