package com.gt.toolbox.spb.webapps.commons.infra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(Include.NON_EMPTY)
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class LocalidadDto {

    @ToDtoLevel(detailLevel = EntityDetailLevel.COMPLETE)
    private ProvinciaDto provincia;
    @ToDtoLevel(detailLevel = EntityDetailLevel.KEY)
    private Integer id;
    @ToDtoLevel(detailLevel = EntityDetailLevel.SELECT)
    private Integer codigo;
    @ToDtoLevel(detailLevel = EntityDetailLevel.SELECT)
    private String nombre;
    @ToDtoLevel(detailLevel = EntityDetailLevel.LIST)
    private String codigoPostal;
    @ToDtoLevel(detailLevel = EntityDetailLevel.LIST)
    private String prefijoTelefonico;

}
