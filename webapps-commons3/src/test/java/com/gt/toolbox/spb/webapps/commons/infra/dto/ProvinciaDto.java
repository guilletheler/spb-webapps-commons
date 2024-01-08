package com.gt.toolbox.spb.webapps.commons.infra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinciaDto {

    @ToDtoLevel(detailLevel = EntityDetailLevel.NEVER)
    private PaisDto pais;
    private Integer id;
    private Integer codigo;
    private String nombre;
}
