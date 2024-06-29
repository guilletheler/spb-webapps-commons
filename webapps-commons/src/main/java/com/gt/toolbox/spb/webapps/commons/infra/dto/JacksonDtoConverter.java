package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JacksonDtoConverter<E, D> extends AbstractDtoConverter<E, D> {

    List<String> collectionProperties;

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

    private D deserializeToDto(String jsonString, Class<?> jsonViewClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        D dto = null;
        if (jsonString != null) {
            dto = mapper.readerWithView(jsonViewClass).readValue(jsonString,
                    dtoClass);
        }

        return dto;
    }

}
