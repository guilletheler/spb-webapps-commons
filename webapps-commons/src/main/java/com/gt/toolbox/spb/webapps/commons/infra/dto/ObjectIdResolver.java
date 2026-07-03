package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public class ObjectIdResolver<E, ID> implements Function<E, ID> {

    boolean discovered = false;
    boolean isOk = false;
    Method idEntity;

    @Override
    public ID apply(E t) {
        try {
            return internalApply(t);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public ID internalApply(E t)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        if (!discovered) {
            this.discoverMethods(t);
        }
        if (!isOk) {
            return null;
        }
        return (ID) idEntity.invoke(t);
    }

    private void discoverMethods(E entity)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {

        discovered = true;

        try {
            idEntity = entity.getClass().getMethod("getId");
        } catch (Exception e) {
            idEntity = entity.getClass().getMethod("id");
        }
        isOk = true;
    }
}
