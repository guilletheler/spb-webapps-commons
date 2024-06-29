package com.gt.toolbox.spb.webapps.commons.infra.dto;

import org.apache.commons.beanutils.Converter;

public class SameConverter implements Converter {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convert(Class<T> type, Object value) {
        return (T) value;
    }

}
