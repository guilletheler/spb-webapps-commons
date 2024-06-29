package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.BooleanPredicateBuilder;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.CollectionPredicateBuilder;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.DatePredicateBuilder;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.DecimalPredicateBuilder;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.IntegerPredicateBuilder;
import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.StringPredicateBuilder;
import com.gt.toolbox.spb.webapps.payload.FilterMeta;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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

	public static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder builder,
			FilterMeta filter) {
		return buildPredicate(root, builder, filter, new ArrayList<>());
	}

	public static <T> Predicate buildPredicate(Path<?> path, CriteriaBuilder builder,
			FilterMeta filter) {
		return buildPredicate(path, builder, filter, new ArrayList<>());
	}

	private static <T> Predicate buildPredicateAgrupador(Path<?> path, CriteriaBuilder builder,
			FilterMeta filter,
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

	private static <T> Predicate buildPredicate(Path<?> path, CriteriaBuilder builder,
			FilterMeta filter,
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

				}

				ret = buildPredicate(path, builder, filter.getValue(),
						Collection.class.isAssignableFrom(curClass));

			}
		}

		if (ret != null && filter.getOperator() != null
				&& filter.getOperator().equalsIgnoreCase("NOT")) {
			ret = ret.not();
		}

		return ret;
	}

	private static Path<?> agregarJoin(Path<?> path, List<String> pathAgregados, String curPath,
			String curKey) {

		pathAgregados.add(curPath);

		// Logger.getLogger(QueryHelper.class.getName()).log(Level.INFO,
		// "Agregando join para " + curPath);

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

	public static <T> Predicate buildPredicate(Path<?> path, CriteriaBuilder builder,
			String value, boolean isCollection) {

		Predicate ret = null;

		var filterMeta = FilterMeta.fromExpr(value);

		ret = buildFiltermetaPredicate(path, builder, filterMeta, isCollection);

		return ret;
	}

	private static <T> Predicate buildFiltermetaPredicate(Path<?> path, CriteriaBuilder builder,
			FilterMeta filterMeta, boolean isCollection) {
		Predicate ret = null;
		if (filterMeta.getChildrens() != null && !filterMeta.getChildrens().isEmpty()) {
			switch (filterMeta.getOperator().toUpperCase()) {
				case "OR":
					var ors = filterMeta.getChildrens().stream().map(
							fm -> buildFiltermetaPredicate(path, builder, fm, isCollection))
							.filter(pr -> pr != null)
							.toList();
					if (!ors.isEmpty()) {
						ret = builder.or(ors.toArray(new Predicate[0]));
					}
					break;
				case "AND":
					var ands = filterMeta.getChildrens().stream().map(
							fm -> buildFiltermetaPredicate(path, builder, fm,
									isCollection))
							.filter(pr -> pr != null)
							.toList();
					if (!ands.isEmpty()) {
						ret = builder.and(ands.toArray(new Predicate[0]));
					}
					break;
				case "NOT":
					var not = buildFiltermetaPredicate(path, builder,
							filterMeta.getChildrens().get(0), isCollection);
					ret = builder.not(not);
					break;

				default:
					if (isCollection) {
						ret = CollectionPredicateBuilder.buildPredicate(builder, path,
								filterMeta.getValue());
					} else {
						ret = buildPredicate(path, builder, filterMeta.getValue());
					}
					break;
			}
		} else {
			if (isCollection) {
				ret = CollectionPredicateBuilder
						.buildPredicate(builder, path, filterMeta.getValue());
			} else {
				ret = buildPredicate(path, builder, filterMeta.getValue());
			}
		}

		return ret;
	}

	private static Predicate buildPredicate(Path<?> path, CriteriaBuilder builder,
			String value) {

		Predicate predicate = null;
		if (IntegerPredicateBuilder.isIntegerClass(path.getJavaType())) {
			predicate = IntegerPredicateBuilder.buildPredicate(builder, path, value);
		} else if (DecimalPredicateBuilder.isDecimalClass(path.getJavaType())) {
			predicate = DecimalPredicateBuilder.buildPredicate(builder, path, value);
		} else if (BooleanPredicateBuilder.isBooleanClass(path.getJavaType())) {
			predicate = BooleanPredicateBuilder.buildPredicate(builder, path, value);
		} else if (DatePredicateBuilder.isDateClass(path.getJavaType())) {
			predicate = DatePredicateBuilder.buildPredicate(builder, path, value);
		}

		if (predicate == null) {
			predicate = StringPredicateBuilder.buildPredicate(builder, path, value);
		}

		return predicate;

	}

	public static Predicate alwaysTrue(CriteriaBuilder builder) {
		return builder.isTrue(builder.literal(true));
	}

	public static Predicate alwaysFalse(CriteriaBuilder builder) {
		return builder.isTrue(builder.literal(false));
	}


}

