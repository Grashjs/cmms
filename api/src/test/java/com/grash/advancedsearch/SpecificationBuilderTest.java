package com.grash.advancedsearch;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpecificationBuilderTest {

    @Mock
    private Root<Object> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Predicate predicate;

    private SpecificationBuilder<Object> builder;

    @BeforeEach
    void setUp() {
        builder = new SpecificationBuilder<>();
    }

    @Test
    void build_withNoFilters_shouldReturnNull() {
        assertNull(builder.build());
    }

    @Test
    void build_withSingleFilterField_shouldReturnSpecification() {
        FilterField filterField = FilterField.builder()
                .field("name")
                .operation("eq")
                .value("test")
                .build();

        builder.with(filterField);
        Specification<Object> spec = builder.build();

        assertNotNull(spec);
    }

    @Test
    void build_withAndSpecification_shouldCombineCorrectly() {
        Specification<Object> andSpec = (root, query, cb) -> predicate;
        builder.with(andSpec);

        Specification<Object> spec = builder.build();
        assertNotNull(spec);
    }

    @Test
    void build_withOrSpecification_shouldCombineCorrectly() {
        Specification<Object> orSpec = (root, query, cb) -> predicate;
        builder.or(orSpec);

        Specification<Object> spec = builder.build();
        assertNotNull(spec);
    }

    @Test
    void build_withFilterFieldAndAndSpec_shouldCombineBoth() {
        FilterField filterField = FilterField.builder()
                .field("status")
                .operation("eq")
                .value("OPEN")
                .build();

        Specification<Object> andSpec = (root, query, cb) -> predicate;

        builder.with(filterField).with(andSpec);
        Specification<Object> spec = builder.build();

        assertNotNull(spec);
    }

    @Test
    void build_withFilterFieldAndOrSpec_shouldCombineBoth() {
        FilterField filterField = FilterField.builder()
                .field("status")
                .operation("eq")
                .value("OPEN")
                .build();

        Specification<Object> orSpec = (root, query, cb) -> predicate;

        builder.with(filterField).or(orSpec);
        Specification<Object> spec = builder.build();

        assertNotNull(spec);
    }

    @Test
    void build_withMultipleFilterFields_shouldReturnSpecification() {
        builder.with(FilterField.builder().field("name").operation("eq").value("test").build())
                .with(FilterField.builder().field("status").operation("eq").value("OPEN").build());

        Specification<Object> spec = builder.build();
        assertNotNull(spec);
    }

    @Test
    void with_shouldReturnBuilderInstance() {
        FilterField filterField = FilterField.builder().field("name").operation("eq").build();
        SpecificationBuilder<Object> returned = builder.with(filterField);
        assertSame(builder, returned);
    }

    @Test
    void build_withAllComponents_shouldNotThrow() {
        FilterField filterField = FilterField.builder()
                .field("name")
                .operation("eq")
                .value("test")
                .build();

        Specification<Object> andSpec = (r, q, cb) -> cb.equal(r.get("name"), "test");
        Specification<Object> orSpec = (r, q, cb) -> cb.equal(r.get("status"), "OPEN");

        builder.with(filterField).with(andSpec).or(orSpec);
        Specification<Object> spec = builder.build();

        assertNotNull(spec);
    }

    @Test
    void chainingWith_shouldBuildAccumulatedFilters() {
        builder.with(FilterField.builder().field("f1").operation("eq").value("v1").build());
        builder.with(FilterField.builder().field("f2").operation("eq").value("v2").build());
        builder.with(FilterField.builder().field("f3").operation("eq").value("v3").build());

        Specification<Object> spec = builder.build();
        assertNotNull(spec);
    }
}
