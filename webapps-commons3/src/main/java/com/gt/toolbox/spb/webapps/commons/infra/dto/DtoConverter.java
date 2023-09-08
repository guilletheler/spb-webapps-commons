package com.gt.toolbox.spb.webapps.commons.infra.dto;

import com.gt.toolbox.spb.webapps.commons.infra.utils.EntityDetailLevel;

public interface DtoConverter<E, D> {
    D toDto(E entity, EntityDetailLevel level);
    // Solo la clave
    D forKey(E entity);
    // Solo los datos necesarios para llenar un combo de selección
    D forSelect(E entity);
    // Solo los datos necesarios para llenar una lista
    D forList(E entity);
    // Todos
    D forEdit(E entity);
    E toNewEntity(D dto);
    void toEntity(E entity, D dto);
    boolean sameKey(E entity, D dto);
}
