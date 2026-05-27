package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * Indica el nivel de conversión de la entidad a dto
 * 
 * @deprecated "Usar mappers con mapstruct"
 */
@Deprecated(since = "1.1", forRemoval = true)
public enum EntityDetailLevel {

    /**
     * Nunca se pasa el valor a dto
     */
    NEVER(new EntityDetailLevel[] {}),
    /**
     * Se serializa siempre
     */
    KEY(new EntityDetailLevel[] {}),
    /**
     * Se serializa para select, list y completo
     */
    SELECT(new EntityDetailLevel[] { KEY }),
    /**
     * Se serializa para list y completo
     */
    LIST(new EntityDetailLevel[] { SELECT }),
    /**
     * Se serializa solo cuando se pide completo
     */
    COMPLETE(new EntityDetailLevel[] { EntityDetailLevel.LIST });

    EntityDetailLevel[] includedLevels;

    @Getter
    Class<?> jsonView;

    EntityDetailLevel(EntityDetailLevel[] included) {
        this.includedLevels = included;
    }

    public Set<EntityDetailLevel> getIncluded() {
        Set<EntityDetailLevel> ret = new HashSet<>();

        ret.add(this);

        for (var level : includedLevels) {
            ret.add(level);
            ret.addAll(level.getIncluded());
        }

        return ret;
    }
}
