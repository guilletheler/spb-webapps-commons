package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gt.toolbox.spb.webapps.commons.infra.dto.EntityDetailLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Copia de la clase del paquete primeng de angular<br>
 * primeng/api/LazyLoadEvent
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class PageRequest implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Integer first;
    private Integer rows;

    private SortMeta[] multiSortMeta;

    private FilterMeta filter;

    private String globalFilter;

    private EntityDetailLevel detailLevel;
}
