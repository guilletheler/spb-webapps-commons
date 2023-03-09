package com.gt.toolbox.spb.webapps.commons.infra.utils;

/**
 * Indica el nivel de conversión de la entidad a dto
 */
public enum EntityDetailLevel {
    
    /**
     * Nunca se pasa el valor a dto
     */
    NEVER,
    /**
     * Entidad completa
     */
    COMPLETE,
    /**
     * Info suficiente para listar
     */
    LIST,
    /**
     * Info suficiente para seleccionar
     */
    SELECT,
    /**
     * Solo los campos clave
     */
    KEY,
}
