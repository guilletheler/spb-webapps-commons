package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterMeta implements Serializable {

    public final static long serialVersionUID = 1L;

    private String fieldName;
    private String value;

    /**
     * en caso que childrens no sea null o de largo 0, los operadores pueden ser AND, OR, NOT caso
     * contrario es Operador SQL, por defecto like para string, = para el resto
     */
    private String operator;

    private List<FilterMeta> childrens;
}
