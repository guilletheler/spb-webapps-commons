package com.gt.toolbox.spb.jpa.repository;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Tuple;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ExtendedRepository<T, ID extends Serializable>
                extends JpaRepository<T, ID> {

        List<Tuple> findAllWithPagination(Specification<T> specs,
                Pageable pageable,
                List<String> fields);
}
