package com.gt.toolbox.spb.webapps.commons.infra.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import com.gt.toolbox.spb.webapps.payload.FilterMeta;
import com.gt.toolbox.spb.webapps.payload.PageRequest;
import com.gt.toolbox.spb.webapps.payload.SortMeta;
import com.gt.toolbox.spb.webapps.payload.SortMeta.SortDirection;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;



public class PageRequestHelper {

    @NonNull
    public static Pageable toPageable(PageRequest pageRequest) {

        if (pageRequest == null) {
            return Pageable.unpaged();
        }

        if (pageRequest.getFirst() == null) {
            pageRequest.setFirst(0);
        }

        if (pageRequest.getRows() == null) {
            pageRequest.setRows(Integer.MAX_VALUE);
        }

        if (pageRequest.getFirst() > 0 && pageRequest.getRows() >= 0
                && pageRequest.getFirst() % pageRequest.getRows() != 0) {
            throw new IllegalArgumentException(
                    "el primer elemento debe ser multiplo de la cantidad de elementos por pagina, first: "
                            + pageRequest.getFirst() + ", rows: " + pageRequest.getRows() +
                            " first % rows = " + (pageRequest.getFirst() % pageRequest.getRows()));
        }
        Sort sorts = null;

        if (pageRequest.getSortField() != null) {
            sorts = Sort.by(pageRequest.getSortField());
            if (pageRequest.getSortDirection() == SortDirection.DESC) {
                sorts = sorts.descending();
            }
        }

        if (pageRequest.getMultiSortMeta() != null) {

            Sort tmpSort = null;
            for (SortMeta sm : pageRequest.getMultiSortMeta()) {
                tmpSort = Sort.by(sm.getField());
                if (sm.getDirection() == SortDirection.DESC) {
                    tmpSort = tmpSort.descending();
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

    @NonNull
    public static <T> Specification<T> toSpecification(PageRequest pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequest();
            pageRequest.setFirst(0);
            pageRequest.setRows(Integer.MAX_VALUE);
        }

        final PageRequest tmp = pageRequest;
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            // query.distinct(true);
            return buildPredicate(tmp, root, builder);
        };
    }

    private static <T> Predicate buildPredicate(PageRequest pageRequest, Root<T> root,
            CriteriaBuilder builder) {
        if (pageRequest == null || pageRequest.getFilter() == null) {
            return QueryHelper.alwaysTrue(builder);
        }

        Predicate ret = buildPredicate(root, builder, pageRequest.getFilter());

        return ret;
    }

    private static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder builder,
            FilterMeta filter) {

        Predicate ret = QueryHelper.buildPredicate(root, builder, filter);

        return ret;
    }
}
