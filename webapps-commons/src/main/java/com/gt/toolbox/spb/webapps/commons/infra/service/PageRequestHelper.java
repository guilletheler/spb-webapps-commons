package com.gt.toolbox.spb.webapps.commons.infra.service;

import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.gt.toolbox.spb.webapps.payload.FilterMeta;
import com.gt.toolbox.spb.webapps.payload.PageRequest;
import com.gt.toolbox.spb.webapps.payload.SortMeta;
import com.gt.toolbox.spb.webapps.payload.SortMeta.SortDirection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public class PageRequestHelper {

    public static Pageable toPageable(PageRequest pageRequest) {

        if(pageRequest == null) {
            return Pageable.unpaged();
        }

        if (pageRequest.getFirst() == null) {
            pageRequest.setFirst(0);
        }

        if (pageRequest.getRows() == null) {
            pageRequest.setRows(Integer.MAX_VALUE);
        }

        if (pageRequest.getFirst() > 0 && pageRequest.getRows() >= 0
                && pageRequest.getRows() % pageRequest.getFirst() != 0) {
            throw new IllegalArgumentException(
                    "el primer elemento debe ser multiplo de la cantidad de elementos por pagina");
        }
        Sort sorts = null;

        if(pageRequest.getSortField() != null && pageRequest.getSortDirection() != null) {
            Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
            sorts = Sort.by(direction, pageRequest.getSortField());
        } else if (pageRequest.getMultiSortMeta() == null || pageRequest.getMultiSortMeta().length == 0) {
            if (pageRequest.getSortField() != null && !pageRequest.getSortField().isEmpty()) {
                if (Optional.ofNullable(pageRequest.getSortDirection())
                        .orElse(SortDirection.NONE) == SortDirection.ASC) {
                    sorts = Sort.by(pageRequest.getSortField());
                } else if (Optional.ofNullable(pageRequest.getSortDirection())
                        .orElse(SortDirection.NONE) == SortDirection.DESC) {
                    sorts = Sort.by(pageRequest.getSortField()).descending();
                }
            }
        } else {
            Sort tmpSort = null;
            for (SortMeta sm : pageRequest.getMultiSortMeta()) {
                if (sm.getDirection() == SortDirection.ASC) {
                    sorts = Sort.by(sm.getField());
                } else if (sm.getDirection() == SortDirection.DESC) {
                    sorts = Sort.by(sm.getField()).descending();
                } else {
                    continue;
                }

                if (sorts == null) {
                    sorts = tmpSort;
                } else {
                    if (tmpSort != null) {
                        sorts = sorts.and(tmpSort);
                    }
                }
            }
        }

        int firstPage = 0;

        int pageSize = pageRequest.getRows();

        if (pageRequest.getRows() < 0) {
            pageSize = Integer.MAX_VALUE;
        }

        if (pageRequest.getFirst() > 0 && pageRequest.getRows() > 0) {
            firstPage = pageRequest.getFirst() / pageRequest.getRows();
        }

        org.springframework.data.domain.PageRequest ret;

        if (sorts == null) {
            ret = org.springframework.data.domain.PageRequest.of(firstPage, pageSize);
        } else {
            ret = org.springframework.data.domain.PageRequest.of(firstPage, pageSize, sorts);
        }

        return ret;
    }

    public static <T> Specification<T> toSpecification(PageRequest pageRequest) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            query.distinct(true);
            return buildPredicate(pageRequest, root, builder);
        };
    }

    private static <T> Predicate buildPredicate(PageRequest pageRequest, Root<T> root, CriteriaBuilder builder) {
        if (pageRequest == null || pageRequest.getFilter() == null) {
            return QueryHelper.alwaysTrue(builder);
        }

        Predicate ret = buildPredicate(root, builder, pageRequest.getFilter());

        return ret;
    }

    private static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder builder, FilterMeta filter) {

        Predicate ret = null;

        if (filter.getFieldName() != null && !filter.getFieldName().isEmpty() && filter.getValue() != null
                && !filter.getValue().isEmpty()) {

            // nodo tipo filtro

            /**
             * Uso QueryHelper porque crea la consulta según tipo de dato
             */
            ret = QueryHelper.buildPredicate(root, builder, filter.getFieldName(), filter.getValue());

        } else if (filter.getOperator() != null && !filter.getOperator().isEmpty()
                && filter.getChildrens() != null && filter.getChildrens().size() > 0) {

            // nodo tipo operador

            switch (filter.getOperator().toLowerCase()) {
                case "and":
                    Predicate tmpAnd;
                    for (FilterMeta child : filter.getChildrens()) {
                        tmpAnd = buildPredicate(root, builder, child);
                        if (ret == null) {
                            ret = tmpAnd;
                        } else {
                            ret = builder.and(ret, tmpAnd);
                        }
                    }
                    break;
                case "or":
                    Predicate tmpOr;
                    for (FilterMeta child : filter.getChildrens()) {
                        tmpOr = buildPredicate(root, builder, child);
                        if (ret == null) {
                            ret = tmpOr;
                        } else {
                            ret = builder.or(ret, tmpOr);
                        }
                    }
                    break;
                case "not":
                    if (filter.getChildrens().size() != 1) {
                        throw new IllegalArgumentException("El operador NOT solo acepta un hijo");
                    }
                    ret = builder.not(buildPredicate(root, builder, filter.getChildrens().get(0)));
                    break;
                default:
                    break;

            }

        } else {
            throw new IllegalArgumentException(
                    "se esperaba un filtro con nombre de campo y valor o con operador y lista de filtros");
        }

        // Logger.getLogger(PageRequestHelper.class.getName()).info("procesando : " + filter);

        return ret;

    }
}
