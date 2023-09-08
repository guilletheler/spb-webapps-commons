package com.gt.toolbox.spb.jpa.service.support;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterMeta implements Serializable {

    public final static long serialVersionUID = 1L;

    private String fieldName;
    private String value;

    /**
     * en caso que childrens no sea null o de largo 0, los operadores pueden ser
     * AND, OR, NOT
     * caso contrario es Operador SQL, por defecto like para string, = para el resto
     */
    private String operator;

    private List<FilterMeta> childrens;
}