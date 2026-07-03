package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.BiPredicate;

public class ObjectsIdComparator<E, D> implements BiPredicate<E, D> {

    boolean discovered = false;
    boolean isOk = false;
    Method idEntity;
    Method idDto;

    public boolean compareObjectsId(E entity, D dto)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        if (!discovered) {
            this.discoverMethods(entity, dto);
        }
        if (!isOk) {
            return false;
        }

        if (entity == null || dto == null) {
            return false;
        }
        return Objects.equals(idEntity.invoke(entity), idDto.invoke(dto));
    }

    private void discoverMethods(E entity, D dto)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {

        discovered = true;

        try {
            idEntity = entity.getClass().getMethod("getId");
        } catch (Exception e) {
            idEntity = entity.getClass().getMethod("id");
        }

        try {
            idDto = dto.getClass().getMethod("getId");
        } catch (Exception e) {
            idDto = dto.getClass().getMethod("id");
        }
        isOk = true;
    }

    @Override
    public boolean test(E t, D u) {
        try {
            return compareObjectsId(t, u);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}
