package com.gt.toolbox.spb.webapps.payload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public static FilterMeta fromExpr(String value) {

        var ret = new FilterMeta();

        String[] groupValues = value.split("\\+\\+");

        ret.setOperator("OR");
        ret.setChildrens(new ArrayList<>());

        for (String groupValue : groupValues) {
            var group = new FilterMeta();
            group.setOperator("OR");
            group.setChildrens(new ArrayList<>());
            ret.getChildrens().add(group);

            String[] orValues = groupValue.split("\\|\\|");

            for (String orValue : orValues) {
                var or = new FilterMeta();
                or.setChildrens(new ArrayList<>());
                or.setOperator("AND");
                group.getChildrens().add(or);

                String[] andValues = orValue.split("\\&\\&");

                for (String andValue : andValues) {
                    var and = new FilterMeta();
                    and.setChildrens(new ArrayList<>());
                    and.setValue(andValue);
                    or.getChildrens().add(and);
                }
            }
        }

        reduceFilterMeta(ret);

        return ret;
    }

    private static void reduceFilterMeta(FilterMeta filterMeta) {

        if (filterMeta.getChildrens() != null
                && !filterMeta.getChildrens().isEmpty()) {
            if (!Optional.ofNullable(filterMeta.getOperator()).orElse("").equalsIgnoreCase("NOT")
                    && filterMeta.getChildrens().size() == 1) {

                var child = filterMeta.getChildrens().get(0);

                filterMeta.setFieldName(child.getFieldName());
                filterMeta.setValue(child.getValue());
                filterMeta.setOperator(child.getOperator());
                filterMeta.setChildrens(child.getChildrens());

                reduceFilterMeta(filterMeta);
            }

            filterMeta.getChildrens().forEach(fm -> reduceFilterMeta(fm));
        }


    }
}
