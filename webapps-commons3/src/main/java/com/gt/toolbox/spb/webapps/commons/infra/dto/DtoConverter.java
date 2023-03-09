package com.gt.toolbox.spb.webapps.commons.infra.dto;

public interface DtoConverter<E, D> {
    D toDto(E entity);
    D forList(E entity);
    E toNewEntity(D dto);
    void toEntity(E entity, D dto);
    boolean sameKey(E entity, D dto);
}
