package com.gt.toolbox.spb.webapps.payload;

/**
 * Indica el nivel de conversión de la entidad a dto
 */
public enum EntityDetailLevel {
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
    KEY;
}
