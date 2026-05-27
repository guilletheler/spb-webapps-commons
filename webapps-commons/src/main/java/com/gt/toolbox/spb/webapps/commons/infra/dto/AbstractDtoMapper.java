package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Clase abstracta que implementa la interface IDtoMapper
 * 
 * @deprecated Utilizar mapstruct en su lugar
 */
@Deprecated
@Log
public abstract class AbstractDtoMapper<E, D> implements IDtoMapper<E, D> {

    Class<E> entityClass;
    Class<D> dtoClass;

    @Getter
    Map<EntityDetailLevel, List<String>> includeProperties = new HashMap<>();

    @Getter
    Map<EntityDetailLevel, List<String>> excludeProperties = new HashMap<>();

    /*
     * Métodos que guardo para aumentar la performance al convertir y no tenér que
     * buscarlos en cada
     * conversión
     */
    Map<String, Method[]> keyMethods;

    @Override
    public boolean sameKey(E entity, D dto) {

        var ret = false;

        if (entity != null && dto != null) {
            try {
                var eMethod = entity.getClass().getMethod("getCodigo");
                var dtoMethod = dto.getClass().getMethod("getCodigo");

                if (eMethod != null && dtoMethod != null) {
                    ret = Objects.equals(eMethod.invoke(entity), dtoMethod.invoke(dto));
                } else {
                    // Si no es con código pruebo con id
                    eMethod = entity.getClass().getMethod("getId");
                    dtoMethod = dto.getClass().getMethod("getId");

                    if (eMethod != null && dtoMethod != null) {
                        ret = Objects.equals(eMethod.invoke(entity), dtoMethod.invoke(dto));
                    }
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                // No se pueden comparar
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

                keyMethods.put(k, new Method[] { entityMethod, dtoMethod });
            } catch (NoSuchMethodException | SecurityException e) {
                log.log(Level.SEVERE, "Error descubriendo key methods");
            }

        }

        return keyMethods;

    }

}
