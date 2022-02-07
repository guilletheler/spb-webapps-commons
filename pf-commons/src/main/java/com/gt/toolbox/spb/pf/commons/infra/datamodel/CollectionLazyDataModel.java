package com.gt.toolbox.spb.pf.commons.infra.datamodel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import com.gt.toolbox.spb.webapps.commons.infra.utils.Utils;
import com.gt.toolbox.spb.webapps.commons.infra.datamodel.LazySorter;
import com.gt.toolbox.spb.webapps.commons.infra.datamodel.ObjectEvaluator;

import lombok.Getter;

public class CollectionLazyDataModel<T> extends LazyDataModel<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Getter
	List<T> datasource;

	Method rowKeyMethod;

	private Class<T> clazz;

	Map<String, ObjectEvaluator<T>> evaluators = new HashMap<String, ObjectEvaluator<T>>();

	public CollectionLazyDataModel(Class<T> clazz, List<T> data) {
		this(clazz, data, "id");
	}

	public CollectionLazyDataModel(Class<T> clazz, List<T> data, String rowKey) {
		super();

		this.datasource = data;
		this.setRowCount(data.size());
		this.clazz = clazz;
		try {
			rowKeyMethod = clazz.getMethod("get" + StringUtils.capitalize(rowKey));
		} catch (NoSuchMethodException | SecurityException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error obteniendo metodo para rowKey");
		}
	}

	public void updateSize() {
		this.setRowCount(datasource.size());
	}

	public void setDatasource(List<T> data) {
		this.datasource = data;
		this.setRowCount(data.size());
	}

	@Override
	public T getRowData(String rowKey) {
		for (T object : datasource) {
			if (Objects.equals(getRowKey(object).toString(), rowKey)) {
				return object;
			}
		}

		return null;
	}

	@Override
	public String getRowKey(T object) {
		try {
			Object tmp = rowKeyMethod.invoke(object);

			if (tmp != null) {
				return tmp.toString();
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error obteniendo columna del objeto");
		}
		return null;
	}

	@Override
	public List<T> load(int first, int pageSize, Map<String, SortMeta> sortMeta, Map<String, FilterMeta> filterMeta) {
		List<T> data = new ArrayList<>();

		// filter
		for (T entity : datasource) {
			boolean match = true;

			// String filtrosLog = filterMeta.isEmpty() ? "Sin filtros" : ("filtrando " +
			// filterMeta.toString());

			if (filterMeta != null) {

				for (FilterMeta meta : filterMeta.values()) {
					try {
						if (meta.getField() == null) {
							Logger.getLogger(getClass().getName()).log(Level.WARNING,
									"column o column.field de meta es nulo " + toString());
							continue;
						}

						String filterField = meta.getField(); // meta.getColumn().getField();
						Object filterValue = meta.getFilterValue();

						if (filterValue == null) {
							// filtrosLog += " filterValue es nulo";
							continue;
						}

						// String methodName = StringUtils.capitalize(filterField);

						match = false;

						for (String s : filterField.split(",|\\s+")) {
							match = match || matchValue(s.trim(), filterValue, entity);
						}

						if (!match) {
							// filtrosLog += " no matchea, descarto";
							break;
						}

					} catch (NullPointerException | IllegalArgumentException
							| SecurityException e) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error al filtrar collection", e);
						match = false;
					}
				}
			}

			// Logger.getLogger(getClass().getName()).log(Level.INFO, filtrosLog);

			if (match) {
				data.add(entity);
			}
		}

		// sort
		if (sortMeta != null && !sortMeta.isEmpty()) {
			for (SortMeta meta : sortMeta.values()) {
				// Collections.sort(data, new LazySorter<>(clazz, meta.getField(),
				// meta.getOrder()));
				Collections.sort(data, new LazySorter<>(clazz, meta.getField(), Objects.equals(meta.getOrder(), SortOrder.ASCENDING) ? 1 : -1));
			}
		}

		// rowCount
		int dataSize = data.size();
		this.setRowCount(dataSize);

		List<T> ret;

		// paginate
		if (dataSize > pageSize) {
			try {
				ret = data.subList(first, first + pageSize);
			} catch (IndexOutOfBoundsException e) {
				ret = data.subList(first, first + (dataSize % pageSize));
			}
		} else {
			ret = data;
		}
		//
		// Logger.getLogger(getClass().getName()).log(Level.INFO,
		// "devolviendo " + ret.size() + " objetos de un total de " +
		// datasource.size());

		return ret;
	}

	private boolean matchValue(String filterField, Object filterValue, T entity) {
		if (!evaluators.containsKey(filterField)) {
			evaluators.put(filterField, new ObjectEvaluator<>(clazz, filterField));
		}

		Object fieldValue = evaluators.get(filterField).evaluate(entity);

		boolean match = false;
		for (String orv : String.valueOf(filterValue).split("\\|\\|")) {
			boolean matchand = true;
			for (String andv : orv.split("\\&\\&")) {
				matchand = matchand && testMatchValue(andv, fieldValue);
			}
			match = match || matchand;
		}

		return match;
	}

	private boolean testMatchValue(String filterValue, Object fieldValue) {
		boolean match;
		if (fieldValue == null) {
			fieldValue = "";
		} 
		
		
		if (Objects.equals(fieldValue.getClass(), Boolean.class)
				|| Objects.equals(fieldValue.getClass(), boolean.class)) {
			Boolean filterVal = filterValue.equalsIgnoreCase("1")
					|| filterValue.equalsIgnoreCase("True")
					|| filterValue.equalsIgnoreCase("V")
					|| filterValue.equalsIgnoreCase("Verdadero")
					|| filterValue.equalsIgnoreCase("Si");

			// filtrosLog += " buscando un valor boolean " + filterVal + " para el campo " +
			// methodName;

			match = Objects.equals((Boolean) fieldValue, filterVal);
		} else if (Objects.equals(fieldValue.getClass(), Date.class)) {
			if (filterValue.contains("-")) {
				// 2 fechas
				String[] fechas = filterValue.split("-");

				Date desde;
				Date hasta;

				try {
					desde = Utils.SDF_SLASH_DMYY.parse(fechas[0]);
				} catch (ParseException e) {
					Calendar cal = Calendar.getInstance();
					cal.set(1900, 0, 1);
					desde = cal.getTime();
				}

				try {
					hasta = Utils.SDF_SLASH_DMYY.parse(fechas[1]);
				} catch (ParseException | ArrayIndexOutOfBoundsException e) {
					hasta = DateUtils.ceiling(new Date(), Calendar.DAY_OF_MONTH);
				}

				Date tmp = (Date) fieldValue;

				// filtrosLog += " buscando un valor fecha entre 2 fechas para el campo " +
				// methodName;

				match = (DateUtils.isSameDay(tmp, desde) || tmp.after(desde))
						&& (DateUtils.isSameDay(tmp, hasta) || tmp.before(hasta));
			} else {
				// filtrosLog += " buscando un valor de fecha para el campo " + methodName;

				match = Utils.SDF_SLASH_DMYY.format((Date) fieldValue)
						.contains(filterValue);
			}
		} else if (Number.class.isAssignableFrom(fieldValue.getClass())) {
			if (filterValue.startsWith("0")) {
				// filtrosLog += " buscando un valor de numero exacto de " + fieldValue + " para
				// el campo " + methodName;
				match = Objects.equals(Double.valueOf(filterValue),
						Double.valueOf(String.valueOf(fieldValue)));
			} else if (filterValue.startsWith("!")) {
				try {
					match = ((Number) fieldValue).doubleValue() != Double
							.valueOf(filterValue.substring(1).trim());
				} catch (NumberFormatException ex) {
					match = false;
				}
			} else if (filterValue.startsWith(">")) {
				try {
					match = ((Number) fieldValue).doubleValue() > Double
							.valueOf(filterValue.substring(1).trim());
				} catch (NumberFormatException ex) {
					match = false;
				}
			} else if (filterValue.startsWith("<")) {
				try {
					match = ((Number) fieldValue).doubleValue() < Double
							.valueOf(filterValue.substring(1).trim());
				} catch (NumberFormatException ex) {
					match = false;
				}
			} else {
				// filtrosLog += " buscando un valor de numero " + fieldValue + " para el campo
				// " + methodName;
				match = String.valueOf(fieldValue).contains(filterValue);
			}
		} else {
			// filtrosLog += " buscando un valor de string " + fieldValue + " para el campo
			// " + methodName;
			if (filterValue.startsWith("!")) {
				match = !matchString(String.valueOf(Optional.ofNullable(fieldValue).orElse("")),
						filterValue.substring(1));
			} else {
				match = matchString(String.valueOf(Optional.ofNullable(fieldValue).orElse("")), filterValue);
			}
		}
		return match;
	}

	private boolean matchString(String fieldValue, String filterValue) {
		if (filterValue.length() > 1 && filterValue.startsWith("'") && filterValue.endsWith("'")) {
			filterValue = filterValue.substring(1, filterValue.length() - 1);
			return fieldValue.equalsIgnoreCase(filterValue);
		}
		return fieldValue.toLowerCase()
				.contains(filterValue.toLowerCase());
	}
}
