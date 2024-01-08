package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.IOException;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public abstract class JacksonDtoConverter<E, D> implements IDtoConverter<E, D> {

    Class<E> entityClass;
    Class<D> dtoClass;

    @Getter
    Map<EntityDetailLevel, List<String>> includeProperties = new HashMap<>();

    @Getter
    Map<EntityDetailLevel, List<String>> excludeProperties = new HashMap<>();

    List<String> collectionProperties;

    /*
     * Métodos que guardo para aumentar la performance al convertir y no tenér que buscarlos en cada
     * conversión
     */
    Map<String, Method[]> keyMethods;
    Map<String, Method[]> collectionMethods;

    Map<EntityDetailLevel, Method[]> setNullExcludeMethods = new HashMap<>();

    /**
     * @param entityClass
     * @param dtoClass
     */
    public JacksonDtoConverter(Class<E> entityClass, Class<D> dtoClass) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
    }

    @Override
    public D toDto(E entity, EntityDetailLevel detailLevel) {

        var mapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString = mapper.writer().writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar entidad", e);
        }

        try {
            return deserializeToDto(jsonString, detailLevel.getJsonView());
        } catch (IOException e) {
            throw new RuntimeException("Error serializando entity a dto", e);
        }
    }

    @Override
    public D forKey(E entity) {
        return toDto(entity, EntityDetailLevel.KEY);
    }

    @Override
    public D forSelect(E entity) {
        return toDto(entity, EntityDetailLevel.SELECT);
    }

    @Override
    public D forList(E entity) {
        return toDto(entity, EntityDetailLevel.LIST);
    }

    @Override
    public D forEdit(E entity) {
        return toDto(entity, EntityDetailLevel.COMPLETE);
    }

    @Override
    public E toNewEntity(D dto) {
        E newEntity = null;
        try {
            newEntity = entityClass.getDeclaredConstructor().newInstance();
            toEntity(newEntity, dto);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Error deserializando dto en entidad", e);
        }

        return newEntity;
    }


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

    private static Map<String, Method[]> discoverKeyMethods(Class<?> entityClass, Class<?> dtoClass,
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

    private static List<String> discoverProperties(Class<?> clazz, EntityDetailLevel level) {
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

    private D deserializeToDto(String jsonString, Class<?> jsonViewClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        D dto = null;
        if (jsonString != null) {
            dto = mapper.readerWithView(jsonViewClass).readValue(jsonString,
                    dtoClass);
        }

        return dto;
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
