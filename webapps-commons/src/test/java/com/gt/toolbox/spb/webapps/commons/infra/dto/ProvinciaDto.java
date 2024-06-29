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
public class ProvinciaDto {

    @ToDtoLevel(detailLevel = EntityDetailLevel.NEVER)
    private PaisDto pais;
    private Integer id;
    private Integer codigo;
    private String nombre;
}
