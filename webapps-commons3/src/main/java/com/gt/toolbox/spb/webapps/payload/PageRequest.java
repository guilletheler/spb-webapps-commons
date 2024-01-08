package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gt.toolbox.spb.webapps.commons.infra.dto.EntityDetailLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de la clase del paquete primeng de angular<br>
 * primeng/api/LazyLoadEvent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class PageRequest implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Integer first;
    private Integer rows;
    private String sortField;
    private SortMeta.SortDirection sortDirection;

    private SortMeta[] multiSortMeta;

    private FilterMeta filter;

    private String globalFilter;

    private EntityDetailLevel detailLevel;
}
