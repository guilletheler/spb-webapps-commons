package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

public class CollectionConverter implements PropertyValueConverter {

    @Getter
    @Setter
    Class<?> clazz = ArrayList.class;

    // @Getter
    // @Setter
    // Class<?> sourceItemClass;

    // @Getter
    // @Setter
    // Class<?> targetItemClass;
    
    // @Getter
    // @Setter
    // EntityDetailLevel itemLevel;
    
    @Getter
    @Setter
    EntityToDtoConverter itemConverter;

    public CollectionConverter() {
    }

    public CollectionConverter(Class<?> clazz) {
        this.clazz = clazz;
    }

    public CollectionConverter(Class<?> clazz, Class<?> targetItemClass, EntityDetailLevel itemLevel) {
        this.clazz = clazz;

        itemConverter = new EntityToDtoConverter(targetItemClass, itemLevel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convertValue(Object target, Object value) {
        if (value == null) {
            if (target != null) {
                ((Collection<Object>) target).clear();
            }
            target = null;
        } else {

            if (target == null) {
                target = newDefaultValue();
            } else {
                ((Collection<Object>) target).clear();
            }

            // Acá hay que ver si hay que transformar cada elemento
            for (Object item : (Collection<Object>) value) {
                if(itemConverter != null) {
                    ((Collection<Object>) target).add(itemConverter.convertValue(null, item));
                } else {
                    ((Collection<Object>) target).add(item);
                }
            }
        }

        return target;
    }

    @Override
    public Object newDefaultValue() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            Utils.logError(getClass(), e);
        }
        return null;
    }

}
