package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IdValueConverter implements PropertyValueConverter {

    @Override
    public Object convertValue(Object target, Object value) {
        if (value != null) {

            try {
                var field = value.getClass().getDeclaredField("id");
                return field.get(value);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, "no se pudo obtener el id del objeto");
            }
        }
        return null;
    }

    @Override
    public Object newDefaultValue() {
        return null;
    }
}
