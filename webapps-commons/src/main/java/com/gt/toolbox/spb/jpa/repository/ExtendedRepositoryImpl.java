package com.gt.toolbox.spb.jpa.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;

public class ExtendedRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> implements ExtendedRepository<T, ID> {

    private EntityManager entityManager;

    public ExtendedRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
            EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    public List<Tuple> findAllWithPagination(Specification<T> specs,
            Pageable pageable,
            List<String> fields) {
        Assert.notNull(pageable, "Pageable must be not null!");
        Assert.notNull(fields, "Fields must not be null!");
        Assert.notEmpty(fields, "Fields must not be empty!");

        // Create query
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        // Define FROM clause
        Root<T> root = applySpecToCriteria(query, builder, specs);
        // Define selecting expression
        List<Selection<?>> selections = getSelections(fields, root);
        query.multiselect(selections);
        // Define ORDER BY clause
        applySorting(builder, query, root, pageable);
        return getPageableResultList(query, pageable);
    }

    private <R> Root<T> applySpecToCriteria(CriteriaQuery<R> query,
            CriteriaBuilder builder,
            Specification<T> specs) {
        Assert.notNull(query, "CriteriaQuery must not be null!");

        Root<T> root = query.from(getDomainClass());

        if (specs == null) {
            return root;
        }

        Predicate predicate = specs.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        return root;
    }

    private List<Selection<?>> getSelections(List<String> fields,
            Root<T> root) {
        List<Selection<?>> selections = new ArrayList<>();

        for (String field : fields) {
            selections.add(root.get(field).alias(field));
        }

        return selections;
    }

    private <R> void applySorting(CriteriaBuilder builder,
            CriteriaQuery<R> query,
            Root<T> root,
            Pageable pageable) {
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        if (sort.isSorted()) {
            query.orderBy(toOrders(sort, root, builder));
        }
    }

    private List<Order> toOrders(Sort sort, Root<T> root, CriteriaBuilder builder) {
        return sort.toList().stream().map(s -> {
            Order o;
            if (s.getDirection() == Direction.DESC) {
                o = builder.desc(root.get(s.getProperty()));
            } else {
                o = builder.asc(root.get(s.getProperty()));
            }
            return o;
        }).collect(Collectors.toList());
    }

    private <R> List<R> getPageableResultList(CriteriaQuery<R> query,
            Pageable pageable) {

        TypedQuery<R> typedQuery = entityManager.createQuery(query);

        // Apply pagination
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        return typedQuery.getResultList();
    }
}
