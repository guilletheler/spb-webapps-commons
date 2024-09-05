package com.gt.toolbox.spb.webapps.commons.infra.datamodel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectEvaluator<T> {

	private static final Logger LOG =
			LoggerFactory.getLogger(ObjectEvaluator.class);

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
				LOG.error("No se puede obtener el método {} en {} para {}", methodName, fieldChain,
						clazz.getName());
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
					LOG.error("Error al evaluar objeto {}", e.getMessage());

					ret = null;
					break;
				}
			}
		}

		return ret;
	}
}
