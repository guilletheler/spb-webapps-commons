package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.lang.reflect.InvocationTargetException;

import lombok.Getter;
import lombok.Setter;

public class EntityToDtoConverter implements PropertyValueConverter {

    @Getter
    @Setter
    EntityDetailLevel level = EntityDetailLevel.SELECT;

    @Getter
    @Setter
    Class<?> targetClass;

    public EntityToDtoConverter(Class<?> targetClass, EntityDetailLevel level) {
        this.targetClass = targetClass;
        this.level = level;
    }

    @Override
    public Object convertValue(Object target, Object value) {

        if (target == null) {
            target = newDefaultValue();
        }

        BeanUtils.fillProperties(value, target, level);

        return target;
    }

    @Override
    public Object newDefaultValue() {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            Utils.logError(getClass(), e);
        }
        return null;
    }

}
