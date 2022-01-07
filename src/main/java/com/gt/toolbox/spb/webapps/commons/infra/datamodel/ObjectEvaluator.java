package com.gt.toolbox.spb.webapps.commons.infra.datamodel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

public class ObjectEvaluator<T> {

	private List<Method> methods;

	public ObjectEvaluator(Class<T> clazz, String fieldChain) {

		String[] chain = fieldChain.split("\\.");

		Class<?> curClass = clazz;
		methods = new ArrayList<>();
		Method method;
		for (String field : chain) {
			String methodName = "get" + StringUtils.capitalize(field);
			try {
				method = curClass.getMethod(methodName);
			} catch (NoSuchMethodException | SecurityException e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE,
						"No se puede obtener el método " + methodName + " en " + fieldChain + " para "
								+ clazz.getName());
				break;
			}
			methods.add(method);
			curClass = method.getReturnType();
		}

	}

	public Object evaluate(T obj) {
		Object ret = obj;

		if (ret != null) {
			for (Method m : methods) {
				try {
					ret = m.invoke(ret);
					if (ret == null) {
						break;
					}
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"Error al evaluar objeto " + e.getMessage());

					ret = null;
					break;
				}
			}
		}

		return ret;
	}
}
