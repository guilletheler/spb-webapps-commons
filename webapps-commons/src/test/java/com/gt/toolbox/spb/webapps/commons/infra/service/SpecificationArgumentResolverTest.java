package com.gt.toolbox.spb.webapps.commons.infra.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.context.request.NativeWebRequest;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SpecificationArgumentResolverTest {

    public static class TestEntity {
        private String name;
        private Integer age;
        private LocalDate date;
        private Boolean active;
        private Set<String> tags;

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public LocalDate getDate() {
            return date;
        }

        public Boolean getActive() {
            return active;
        }

        public Set<String> getTags() {
            return tags;
        }
    }

    public void dummyMethod(Specification<TestEntity> spec) {
    }

    private NativeWebRequest request;
    private SpecificationArgumentResolver resolver;
    private MethodParameter parameter;

    private Root<Object> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    @BeforeEach
    public void setup() throws Exception {
        request = mock(NativeWebRequest.class);
        resolver = new SpecificationArgumentResolver();
        parameter = MethodParameter.forExecutable(
                SpecificationArgumentResolverTest.class.getMethod("dummyMethod", Specification.class), 0);

        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);

        doReturn(TestEntity.class).when(root).getJavaType();
        doReturn(new java.util.HashSet<>()).when(root).getJoins();
    }

    private Path<?> mockPath(String fieldName, Class<?> fieldType) {
        Path path = mock(Path.class);
        doReturn(fieldType).when(path).getJavaType();
        doReturn(path).when(root).get(eq(fieldName));
        return path;
    }

    @Test
    public void testStringEquals() throws Exception {
        when(request.getParameter("filter")).thenReturn("name==John");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        var path = mockPath("name", String.class);
        Expression exprUpper = mock(Expression.class);
        Expression exprStr = mock(Expression.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(path.as(String.class)).thenReturn(exprStr);
        when(cb.upper(exprStr)).thenReturn(exprUpper);
        when(cb.like(exprUpper, "%JOHN%")).thenReturn(expectedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).like(exprUpper, "%JOHN%");
    }

    @Test
    public void testIntegerGreaterThan() throws Exception {
        when(request.getParameter("filter")).thenReturn("age=gt=18");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        Path path = mockPath("age", Integer.class);
        Expression exprInt = mock(Expression.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(path.as(java.math.BigInteger.class)).thenReturn(exprInt);
        when(cb.greaterThan(eq(exprInt), eq(java.math.BigInteger.valueOf(18L)))).thenReturn(expectedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).greaterThan(eq(exprInt), eq(java.math.BigInteger.valueOf(18L)));
    }

    @Test
    public void testBooleanEquals() throws Exception {
        when(request.getParameter("filter")).thenReturn("active==true");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        Path path = mockPath("active", Boolean.class);
        Expression exprCoalesce = mock(Expression.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(cb.coalesce(path, Boolean.FALSE)).thenReturn(exprCoalesce);
        when(cb.equal(exprCoalesce, Boolean.TRUE)).thenReturn(expectedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(exprCoalesce, Boolean.TRUE);
    }

    @Test
    public void testStringIn() throws Exception {
        when(request.getParameter("filter")).thenReturn("name=in=(Alice,Bob)");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        Path path = mockPath("name", String.class);
        Expression exprUpper = mock(Expression.class);
        Expression exprStr = mock(Expression.class);
        Predicate pred1 = mock(Predicate.class);
        Predicate pred2 = mock(Predicate.class);
        Predicate orPred = mock(Predicate.class);

        when(path.as(String.class)).thenReturn(exprStr);
        when(cb.upper(exprStr)).thenReturn(exprUpper);

        when(cb.like(exprUpper, "%ALICE%")).thenReturn(pred1);
        when(cb.like(exprUpper, "%BOB%")).thenReturn(pred2);
        // Current implementation generates an OR array
        when(cb.or(any(Predicate[].class))).thenReturn(orPred);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);

        // it builds with ORs for each =in=
        verify(cb).like(exprUpper, "%ALICE%");
        verify(cb).like(exprUpper, "%BOB%");
    }

    @Test
    public void testStringLike() throws Exception {
        when(request.getParameter("filter")).thenReturn("name=like=*ohn*");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        Path path = mockPath("name", String.class);
        Expression exprStr = mock(Expression.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(path.as(String.class)).thenReturn(exprStr);
        // =like=*ohn* gives mapSingleValue -> "'*ohn*'" wait!
        // cleanVal.contains("*") returns "'" + cleanVal.replace('*', '%') + "'"
        // StringPredicateBuilder sees it starts with "'" so uses value without "%" and
        // no upper
        when(cb.like(exprStr, "%ohn%")).thenReturn(expectedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).like(exprStr, "%ohn%");
    }

    @Test
    public void testNotEqual() throws Exception {
        when(request.getParameter("filter")).thenReturn("name!=Alice");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        Path path = mockPath("name", String.class);
        Expression exprUpper = mock(Expression.class);
        Expression exprStr = mock(Expression.class);
        Predicate eqPredicate = mock(Predicate.class);
        Predicate notPredicate = mock(Predicate.class);

        when(path.as(String.class)).thenReturn(exprStr);
        when(cb.upper(exprStr)).thenReturn(exprUpper);

        when(cb.like(exprUpper, "%ALICE%")).thenReturn(eqPredicate);
        when(cb.not(eqPredicate)).thenReturn(notPredicate);
        when(eqPredicate.not()).thenReturn(notPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).like(exprUpper, "%ALICE%");
    }

    @Test
    public void testComplexAndOr() throws Exception {
        // (name==Alice;age=gt=20),active==true
        when(request.getParameter("filter")).thenReturn("(name==Alice;age=gt=20),active==true");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        Path pathName = mockPath("name", String.class);
        Path pathAge = mockPath("age", Integer.class);
        Path pathActive = mockPath("active", Boolean.class);

        Expression exprUpperName = mock(Expression.class);
        Expression exprStrName = mock(Expression.class);
        when(pathName.as(String.class)).thenReturn(exprStrName);
        when(cb.upper(exprStrName)).thenReturn(exprUpperName);
        Predicate namePred = mock(Predicate.class);
        when(cb.like(exprUpperName, "%ALICE%")).thenReturn(namePred);

        Expression exprIntAge = mock(Expression.class);
        when(pathAge.as(java.math.BigInteger.class)).thenReturn(exprIntAge);
        Predicate agePred = mock(Predicate.class);
        when(cb.greaterThan(eq(exprIntAge), eq(java.math.BigInteger.valueOf(20L)))).thenReturn(agePred);

        Expression exprCoalesce = mock(Expression.class);
        Predicate activePred = mock(Predicate.class);
        when(cb.coalesce(pathActive, Boolean.FALSE)).thenReturn(exprCoalesce);
        when(cb.equal(exprCoalesce, Boolean.TRUE)).thenReturn(activePred);

        // We only care that the predicates are generated properly at the leaves,
        // and that they don't crash.
        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);

        verify(cb).like(exprUpperName, "%ALICE%");
        verify(cb).greaterThan(eq(exprIntAge), eq(java.math.BigInteger.valueOf(20L)));
        verify(cb).equal(exprCoalesce, Boolean.TRUE);
    }

    @Test
    public void testSpecificationArgumentResolverWithPartialDate() throws Exception {
        when(request.getParameter("filter")).thenReturn("date==%06/2026%");
        Specification<Object> spec = (Specification<Object>) resolver.resolveArgument(parameter, null, request, null);

        assertNotNull(spec);

        mockPath("date", LocalDate.class);

        Expression<String> toCharExpr = mock(Expression.class);
        Expression<String> literalExpr = mock(Expression.class);
        doReturn(literalExpr).when(cb).literal(any());
        doReturn(toCharExpr).when(cb).function(eq("to_char"), eq(String.class), any(Expression[].class));

        Predicate expectedPredicate = mock(Predicate.class);
        when(cb.like(eq(toCharExpr), eq("%%06/2026%%"))).thenReturn(expectedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).like(eq(toCharExpr), eq("%%06/2026%%"));
    }
}
