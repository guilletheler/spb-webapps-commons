package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.Optional;

public interface IDtoMapper<E, D> {

    default D toDto(E entity, EntityDetailLevel level) {
        return Optional.ofNullable(entity)
                .map(e -> switch (Optional.ofNullable(level).orElse(EntityDetailLevel.COMPLETE)) {
                    case NEVER -> null;
                    case KEY -> forKey(e);
                    case SELECT -> forSelect(e);
                    case LIST -> forList(e);
                    case COMPLETE -> forEdit(e);
                    default -> throw new IllegalArgumentException("Level no soportado");
                })
                .orElse(null);
    }

    // Solo la clave
    D forKey(E entity);

    // Solo los datos necesarios para llenar un combo de selección
    D forSelect(E entity);

    // Solo los datos necesarios para llenar una lista
    D forList(E entity);

    // Todos
    D forEdit(E entity);

    E toNewEntity(D dto);

    E toKeyEntity(D dto);

    E toEntity(E entity, D dto);

    boolean sameKey(E entity, D dto);

}
