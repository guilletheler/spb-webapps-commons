package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.HashSet;
import java.util.Set;
import com.gt.toolbox.spb.webapps.payload.jsonViews.ForEditJsonView;
import com.gt.toolbox.spb.webapps.payload.jsonViews.ForKeyJsonView;
import com.gt.toolbox.spb.webapps.payload.jsonViews.ForListJsonView;
import com.gt.toolbox.spb.webapps.payload.jsonViews.ForSelectJsonView;
import com.gt.toolbox.spb.webapps.payload.jsonViews.NeverJsonView;
import lombok.Getter;

/**
 * Indica el nivel de conversión de la entidad a dto
 */
public enum EntityDetailLevel {

    /**
     * Nunca se pasa el valor a dto
     */
    NEVER(NeverJsonView.class, new EntityDetailLevel[] {}),
    /**
     * Se serializa siempre
     */
    KEY(ForKeyJsonView.class, new EntityDetailLevel[] {}),
    /**
     * Se serializa para select, list y completo
     */
    SELECT(ForSelectJsonView.class, new EntityDetailLevel[] {KEY}),
    /**
     * Se serializa para list y completo
     */
    LIST(ForListJsonView.class, new EntityDetailLevel[] {SELECT}),
    /**
     * Se serializa solo cuando se pide completo
     */
    COMPLETE(ForEditJsonView.class, new EntityDetailLevel[] {EntityDetailLevel.LIST});

    EntityDetailLevel[] includedLevels;

    @Getter
    Class<?> jsonView;

    EntityDetailLevel(Class<?> jsonView, EntityDetailLevel[] included) {
        this.includedLevels = included;
        this.jsonView = jsonView;
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

    public static Set<EntityDetailLevel> fromJsonView(Class<?> jsonView) {
        Set<EntityDetailLevel> ret = new HashSet<>();

        for (var level : EntityDetailLevel.values()) {
            for (var il : level.getIncluded()) {
                if (il.getJsonView().equals(jsonView)) {
                    ret.add(il);
                }
            }
        }

        return ret;
    }
}
