package com.gt.toolbox.spb.webapps.commons.infra.dto;

import com.gt.toolbox.spb.webapps.commons.infra.utils.EntityDetailLevel;

public interface DtoConverter<E, D> {
    D toDto(E entity, EntityDetailLevel detailLevel);
    D forEdit(E entity);
    D forList(E entity);
    D forSelect(E entity);
    D forKey(E entity);
    E toNewEntity(D dto);
    void toEntity(E entity, D dto);
    boolean sameKey(E entity, D dto);
}
