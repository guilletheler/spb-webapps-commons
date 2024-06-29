package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public abstract class AbstractDtoConverter<E, D> implements IDtoConverter<E, D> {

    Class<E> entityClass;
    Class<D> dtoClass;

    @Getter
    Map<EntityDetailLevel, List<String>> includeProperties = new HashMap<>();

    @Getter
    Map<EntityDetailLevel, List<String>> excludeProperties = new HashMap<>();

    /*
     * Métodos que guardo para aumentar la performance al convertir y no tenér que buscarlos en cada
     * conversión
     */
    Map<String, Method[]> keyMethods;


    @Override
    public boolean sameKey(E entity, D dto) {

        if (keyMethods == null) {
            if (includeProperties.get(EntityDetailLevel.KEY) == null) {
                includeProperties.put(EntityDetailLevel.KEY,
                        discoverProperties(dtoClass, EntityDetailLevel.KEY));
            }

            keyMethods = discoverKeyMethods(entityClass, dtoClass,
                    includeProperties.get(EntityDetailLevel.KEY));
        }

        boolean ret = true;

        for (var ke : keyMethods.entrySet()) {
            try {
                var entityKeyVal = keyMethods.get(ke.getKey())[0].invoke(entity);
                var dtoKeyVal = keyMethods.get(ke.getKey())[0].invoke(dto);

                ret = ret && Objects.equals(entityKeyVal, dtoKeyVal);

            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                log.log(Level.SEVERE, "Error al obtener valor de key", e);
                ret = false;
                break;
            }
        }

        return ret;
    }

    protected static Map<String, Method[]> discoverKeyMethods(Class<?> entityClass,
            Class<?> dtoClass,
            List<String> includeProperties) {

        Map<String, Method[]> keyMethods = new HashMap<>();

        for (var k : includeProperties) {
            var methodName = "get" + StringUtils.capitalize(k);

            try {
                var dtoMethod = dtoClass.getDeclaredMethod(methodName);
                var entityMethod = entityClass.getDeclaredMethod(methodName);

                keyMethods.put(k, new Method[] {entityMethod, dtoMethod});
            } catch (NoSuchMethodException | SecurityException e) {
                log.log(Level.SEVERE, "Error descubriendo key methods");
            }

        }

        return keyMethods;

    }

    protected static List<String> discoverProperties(Class<?> clazz, EntityDetailLevel level) {
        List<String> keyProperties = new ArrayList<>();
        for (var f : clazz.getDeclaredFields()) {
            var jsonViewAnnotation = f.getAnnotation(JsonView.class);
            if (jsonViewAnnotation != null) {
                var jsonViewAnnotations = Arrays.asList(jsonViewAnnotation.value());

                if (jsonViewAnnotations.stream().anyMatch(ejv -> EntityDetailLevel.fromJsonView(ejv)
                        .stream().anyMatch(l -> l == level))) {
                    keyProperties.add(f.getName());
                }

            }
        }

        return keyProperties;
    }

    public void discoverDtoIncludeProperties() {
        this.includeProperties.put(EntityDetailLevel.KEY,
                discoverProperties(dtoClass, EntityDetailLevel.KEY));
        this.includeProperties.put(EntityDetailLevel.SELECT,
                discoverProperties(dtoClass, EntityDetailLevel.SELECT));
        this.includeProperties.put(EntityDetailLevel.LIST,
                discoverProperties(dtoClass, EntityDetailLevel.LIST));
        this.includeProperties.put(EntityDetailLevel.COMPLETE,
                discoverProperties(dtoClass, EntityDetailLevel.COMPLETE));
    }
}
