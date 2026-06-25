package com.gt.toolbox.spb.webapps.commons.infra.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.data.jpa.domain.Specification;

import com.gt.toolbox.spb.webapps.commons.infra.service.predicate.builders.DatePredicateBuilder;

import jakarta.persistence.criteria.*;

public class QueryHelperTest {

    public static class TestEntity {
        private LocalDate fecha;
        public LocalDate getFecha() { return fecha; }
    }

    public void dummyMethod(Specification<TestEntity> spec) {}

    @Test
    public void testQueryHelperPartialDateFallback() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        doReturn(LocalDate.class).when(path).getJavaType();

        Expression<String> toCharExpr = mock(Expression.class);
        Expression<String> literalExpr = mock(Expression.class);
        
        doReturn(literalExpr).when(cb).literal(any());
        doReturn(toCharExpr).when(cb).function(eq("to_char"), eq(String.class), any(Expression[].class));
        
        Predicate expectedPredicate = mock(Predicate.class);
        when(cb.like(eq(toCharExpr), eq("%06/2026%"))).thenReturn(expectedPredicate);

        // Test QueryHelper / DatePredicateBuilder.buildSinglePredicate directly with a partial date string starting with =
        // but not parseable as a ZonedDateTime/LocalDate
        Predicate result = DatePredicateBuilder.buildSinglePredicate(cb, path, "=06/2026");
        
        assertNotNull(result);
        assertEquals(expectedPredicate, result);
        verify(cb).like(eq(toCharExpr), eq("%06/2026%"));
    }

    @Test
    public void testSpecificationArgumentResolverWithPartialDate() throws Exception {
        NativeWebRequest request = mock(NativeWebRequest.class);
        // Test filter value similar to the user's scenario
        when(request.getParameter("filter")).thenReturn("fecha==%06/2026%");

        MethodParameter parameter = MethodParameter.forExecutable(
            QueryHelperTest.class.getMethod("dummyMethod", Specification.class), 0);

        SpecificationArgumentResolver resolver = new SpecificationArgumentResolver();
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        assertNotNull(spec);

        Root<Object> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);

        doReturn(TestEntity.class).when(root).getJavaType();
        doReturn(new java.util.HashSet<>()).when(root).getJoins();
        doReturn(LocalDate.class).when(path).getJavaType();
        // when resolving path "fecha" on root, it should return our path
        doReturn(path).when(root).get(eq("fecha"));

        Expression<String> toCharExpr = mock(Expression.class);
        Expression<String> literalExpr = mock(Expression.class);
        doReturn(literalExpr).when(cb).literal(any());
        doReturn(toCharExpr).when(cb).function(eq("to_char"), eq(String.class), any(Expression[].class));

        Predicate expectedPredicate = mock(Predicate.class);
        when(cb.like(eq(toCharExpr), eq("%%06/2026%%"))).thenReturn(expectedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(expectedPredicate, result);
        verify(cb).like(eq(toCharExpr), eq("%%06/2026%%"));
    }
}
