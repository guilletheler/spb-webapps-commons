package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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

    Integer first;
    Integer rows;
    String sortField;
    SortMeta.SortDirection sortDirection;

    SortMeta[] multiSortMeta;

    FilterMeta filter;

    String globalFilter;
    
}
