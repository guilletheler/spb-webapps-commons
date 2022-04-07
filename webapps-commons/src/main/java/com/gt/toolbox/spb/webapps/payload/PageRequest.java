package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gt.toolbox.spb.webapps.commons.infra.service.QueryHelper;
import com.gt.toolbox.spb.webapps.payload.SortMeta.SortDirection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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
