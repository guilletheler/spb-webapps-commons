package com.gt.toolbox.spb.webapps.commons.infra.utils;

public interface PropertyValueConverter {
    
    /**
     * Si target no es nulo intenta reemplazar los valores internos de target, sinó crea un nuevo objeto
     * @param target
     * @param value
     * @return target
     */
    public Object convertValue(Object target, Object value);

    public Object newDefaultValue();
}
