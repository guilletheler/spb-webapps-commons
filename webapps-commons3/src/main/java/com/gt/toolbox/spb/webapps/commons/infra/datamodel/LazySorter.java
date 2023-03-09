package com.gt.toolbox.spb.webapps.commons.infra.datamodel;

import java.util.Comparator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LazySorter<T> implements Comparator<T> {


	private ObjectEvaluator<T> objectEvaluator;
	private Integer sortOrder;

	public LazySorter(Class<T> clazz, String sortField, Integer sortOrder) {
		this.sortOrder = sortOrder;
		objectEvaluator = new ObjectEvaluator<T>(clazz, sortField);
	}

	public int compare(T obj1, T obj2) {
		try {
			
			Object value1 = objectEvaluator.evaluate(obj1);
			Object value2 = objectEvaluator.evaluate(obj2);
			
			if(value1 == null && value2 == null) {
				return 0;
			} else if(value1 == null) {
				return -1;
			} else if(value2 == null) {
				return 1;
			} 
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			int value = ((Comparable) value1).compareTo(value2);

			if(Optional.ofNullable(sortOrder).orElse(0) >= 0) {
				value = value * -1;
			}

			return value;
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error al aplicar filtro", e);

			return 0;
		}
	}
}