package com.grash.advancedsearch;

import com.grash.model.enums.EnumName;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.utils.Helper;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WrapperSpecificationTest {

    @jakarta.persistence.Entity
    static class TestEntity {
        private Long id;
    }

    @Mock(answer = Answers.RETURNS_DEFAULTS)
    private Root<Object> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;
    @Mock
    private Path simplePath;
    @Mock
    private Path entityIdPath;
    @Mock
    private Expression<String> stringExpression;
    @Mock
    private Expression<String> lowerExpression;
    @Mock
    private Expression<String> literalExpression;
    @Mock
    private Predicate mockPredicate;
    @Mock
    private EntityType<Object> entityType;
    @Mock
    private Attribute<?, ?> nonCollectionAttribute;
    @Mock
    private Attribute<?, ?> collectionAttribute;
    @Mock
    private Join<Object, Object> join;
    @Mock
    private From<Object, Object> fromPath;

    @BeforeEach
    void setUp() {
        lenient().when(root.getModel()).thenReturn(entityType);
        lenient().when(nonCollectionAttribute.isCollection()).thenReturn(false);
        lenient().when(collectionAttribute.isCollection()).thenReturn(true);
        for (String attr : new String[]{"name", "status", "priority", "createdAt",
                "details", "description", "relatedEntity", "owner"}) {
            lenient().when(entityType.getAttribute(attr)).thenReturn((Attribute) nonCollectionAttribute);
        }
        lenient().when(entityType.getAttribute("categories")).thenReturn((Attribute) collectionAttribute);
        lenient().when(entityType.getAttribute("id")).thenReturn((Attribute) nonCollectionAttribute);
        lenient().when(root.get("name")).thenReturn(simplePath);
        lenient().when(root.get("status")).thenReturn(simplePath);
        lenient().when(root.get("priority")).thenReturn(simplePath);
        lenient().when(root.get("createdAt")).thenReturn(simplePath);
        lenient().when(root.get("relatedEntity")).thenReturn(simplePath);
        lenient().when(root.get("owner")).thenReturn(fromPath);
        lenient().when(simplePath.getJavaType()).thenReturn((Class) String.class);
        lenient().doReturn(entityType).when(join).getModel();
        lenient().doReturn(entityType).when(fromPath).getModel();
        lenient().when(fromPath.get("name")).thenReturn(simplePath);
        lenient().when(entityIdPath.getJavaType()).thenReturn((Class) String.class);

        lenient().when(cb.lower(any())).thenReturn(lowerExpression);
        lenient().when(cb.function(eq("unaccent"), eq(String.class), any())).thenReturn(stringExpression);
        lenient().when(cb.literal(anyString())).thenReturn(literalExpression);
        lenient().when(cb.like(any(Expression.class), any(Expression.class))).thenReturn(mockPredicate);
    }

    private Predicate buildSpec(FilterField filterField) {
        return new WrapperSpecification<>(filterField).toPredicate(root, query, cb);
    }

    private FilterField createFilterField(String field, String operation, Object value) {
        return FilterField.builder()
                .field(field)
                .operation(operation)
                .value(value)
                .build();
    }

    @Nested
    class LikeOperations {

        @Test
        void contains_shouldCreateLikePredicate() {
            Predicate result = buildSpec(createFilterField("name", "cn", "test"));

            assertNotNull(result);
            verify(cb).function(eq("unaccent"), eq(String.class), eq(lowerExpression));
            verify(cb).function(eq("unaccent"), eq(String.class), eq(literalExpression));
        }

        @Test
        void doesNotContain_shouldCreateNotLikePredicate() {
            assertNotNull(buildSpec(createFilterField("name", "nc", "test")));
            verify(cb).not(any());
        }

        @Test
        void beginsWith_shouldCreateLikePredicate() {
            assertNotNull(buildSpec(createFilterField("name", "bw", "test")));
            verify(cb).literal("test%");
        }

        @Test
        void doesNotBeginWith_shouldCreateNotLikePredicate() {
            assertNotNull(buildSpec(createFilterField("name", "bn", "test")));
            verify(cb).not(any());
        }

        @Test
        void endsWith_shouldCreateLikePredicate() {
            assertNotNull(buildSpec(createFilterField("name", "ew", "test")));
            verify(cb).literal("%test");
        }

        @Test
        void doesNotEndWith_shouldCreateNotLikePredicate() {
            assertNotNull(buildSpec(createFilterField("name", "en", "test")));
            verify(cb).not(any());
        }
    }

    @Nested
    class EqualityOperations {

        @Test
        void equal_withNonEntityField_shouldUseDirectEquality() {
            assertNotNull(buildSpec(createFilterField("name", "eq", "testValue")));
            verify(cb).equal(simplePath, "testValue");
        }

        @Test
        void equal_withEntityField_shouldUseIdEquality() {
            when(simplePath.getJavaType()).thenReturn((Class) TestEntity.class);
            when(simplePath.get("id")).thenReturn(entityIdPath);

            assertNotNull(buildSpec(createFilterField("relatedEntity", "eq", 1L)));
            verify(cb).equal(entityIdPath, 1L);
        }

        @Test
        void notEqual_withNonEntityField_shouldUseDirectInequality() {
            assertNotNull(buildSpec(createFilterField("name", "ne", "testValue")));
            verify(cb).notEqual(simplePath, "testValue");
        }

        @Test
        void notEqual_withEntityField_shouldUseIdInequality() {
            when(simplePath.getJavaType()).thenReturn((Class) TestEntity.class);
            when(simplePath.get("id")).thenReturn(entityIdPath);

            assertNotNull(buildSpec(createFilterField("relatedEntity", "ne", 1L)));
            verify(cb).notEqual(entityIdPath, 1L);
        }
    }

    @Nested
    class NullOperations {

        @Test
        void nul_shouldCreateIsNullPredicate() {
            assertNotNull(buildSpec(createFilterField("name", "nu", null)));
            verify(cb).isNull(simplePath);
        }

        @Test
        void notNull_shouldCreateIsNotNullPredicate() {
            assertNotNull(buildSpec(createFilterField("name", "nn", null)));
            verify(cb).isNotNull(simplePath);
        }
    }

    @Nested
    class ComparisonOperations {

        @Test
        void greaterThan_shouldCreateGtPredicate() {
            assertNotNull(buildSpec(createFilterField("createdAt", "gt", "2024-01-01")));
            verify(cb).greaterThan(eq(simplePath), any(Comparable.class));
        }

        @Test
        void greaterThanEqual_withoutEnum_shouldCreateGePredicate() {
            assertNotNull(buildSpec(createFilterField("createdAt", "ge", "2024-01-01")));
            verify(cb).greaterThanOrEqualTo(eq(simplePath), any(Comparable.class));
        }

        @Test
        void greaterThanEqual_withJsDate_shouldConvertDate() {
            String jsDate = "2024-01-15T10:30:00.000Z";
            Date expectedDate = Helper.getDateFromJsString(jsDate);
            FilterField filterField = FilterField.builder()
                    .field("createdAt").operation("ge").value(jsDate).enumName(EnumName.JS_DATE).build();

            assertNotNull(buildSpec(filterField));
            verify(cb).greaterThanOrEqualTo(eq(simplePath), eq(expectedDate));
        }

        @Test
        void lessThan_shouldCreateLtPredicate() {
            assertNotNull(buildSpec(createFilterField("createdAt", "lt", "2024-01-01")));
            verify(cb).lessThan(eq(simplePath), any(Comparable.class));
        }

        @Test
        void lessThanEqual_withoutEnum_shouldCreateLePredicate() {
            assertNotNull(buildSpec(createFilterField("createdAt", "le", "2024-01-01")));
            verify(cb).lessThanOrEqualTo(eq(simplePath), any(Comparable.class));
        }

        @Test
        void lessThanEqual_withJsDate_shouldConvertDate() {
            String jsDate = "2024-01-15T10:30:00.000Z";
            Date expectedDate = Helper.getDateFromJsString(jsDate);
            FilterField filterField = FilterField.builder()
                    .field("createdAt").operation("le").value(jsDate).enumName(EnumName.JS_DATE).build();

            assertNotNull(buildSpec(filterField));
            verify(cb).lessThanOrEqualTo(eq(simplePath), eq(expectedDate));
        }
    }

    @Nested
    class InOperations {

        @Test
        void in_withNonEntityField_shouldUseDirectIn() {
            FilterField filterField = FilterField.builder()
                    .field("name").operation("in").values(Arrays.asList("val1", "val2")).build();

            assertNotNull(buildSpec(filterField));
            verify(cb).in(simplePath);
            verify(cb.in(simplePath)).value("val1");
            verify(cb.in(simplePath)).value("val2");
        }

        @Test
        void in_withEntityField_shouldUseIdIn() {
            when(simplePath.getJavaType()).thenReturn((Class) TestEntity.class);
            when(simplePath.get("id")).thenReturn(entityIdPath);

            FilterField filterField = FilterField.builder()
                    .field("relatedEntity").operation("in").values(Arrays.asList(1L, 2L)).build();

            assertNotNull(buildSpec(filterField));
            verify(cb).in(entityIdPath);
        }

        @Test
        void in_withPriorityEnum_shouldConvertValues() {
            FilterField filterField = FilterField.builder()
                    .field("priority").operation("in").enumName(EnumName.PRIORITY)
                    .values(Arrays.asList("High", "Low")).build();

            assertNotNull(buildSpec(filterField));
            verify(cb.in(simplePath)).value(Priority.HIGH);
            verify(cb.in(simplePath)).value(Priority.LOW);
        }

        @Test
        void in_withStatusEnum_shouldConvertValues() {
            FilterField filterField = FilterField.builder()
                    .field("status").operation("in").enumName(EnumName.STATUS)
                    .values(Arrays.asList("Open", "Complete")).build();

            assertNotNull(buildSpec(filterField));
            verify(cb.in(simplePath)).value(Status.OPEN);
            verify(cb.in(simplePath)).value(Status.COMPLETE);
        }

        @Test
        void in_withJsDateEnum_shouldConvertValues() {
            String jsDate = "2024-01-15T10:30:00.000Z";
            Date expectedDate = Helper.getDateFromJsString(jsDate);
            FilterField filterField = FilterField.builder()
                    .field("createdAt").operation("in").enumName(EnumName.JS_DATE)
                    .values(Arrays.asList(jsDate)).build();

            assertNotNull(buildSpec(filterField));
            verify(cb.in(simplePath)).value(expectedDate);
        }

        @Test
        void inManyToMany_shouldUseJoin() {
            when(root.join(anyString(), any(JoinType.class))).thenReturn(join);

            FilterField filterField = FilterField.builder()
                    .field("categories").operation("inm").joinType(JoinType.LEFT)
                    .values(Arrays.asList(1L, 2L)).build();

            assertNotNull(buildSpec(filterField));
            verify(root).join("categories", JoinType.LEFT);
            verify(cb.in(join.get("id"))).value(1L);
            verify(cb.in(join.get("id"))).value(2L);
        }

        @Test
        void in_withEnumAndNonStringValue_shouldNotConvert() {
            FilterField filterField = FilterField.builder()
                    .field("priority").operation("in").enumName(EnumName.PRIORITY)
                    .values(Arrays.asList(1, 2)).build();

            assertNotNull(buildSpec(filterField));
            verify(cb.in(simplePath)).value(1);
            verify(cb.in(simplePath)).value(2);
        }
    }

    @Nested
    class Alternatives {

        @Test
        void withAlternatives_shouldCombineWithOr() {
            FilterField alternative = FilterField.builder()
                    .field("status").operation("eq").value("COMPLETE").build();
            FilterField mainField = FilterField.builder()
                    .field("status").operation("eq").value("OPEN")
                    .alternatives(List.of(alternative)).build();

            assertNotNull(buildSpec(mainField));
            verify(cb).or(any(Predicate[].class));
        }

        @Test
        void withoutAlternatives_shouldNotWrapWithOr() {
            assertNotNull(buildSpec(createFilterField("name", "eq", "test")));
            verify(cb, never()).or(any(Predicate[].class));
        }

        @Test
        void withEmptyAlternatives_shouldNotWrapWithOr() {
            FilterField filterField = FilterField.builder()
                    .field("status").operation("eq").value("OPEN")
                    .alternatives(List.of()).build();

            assertNotNull(buildSpec(filterField));
            verify(cb, never()).or(any(Predicate[].class));
        }

        @Test
        void withMultipleAlternatives_shouldCombineAllWithOr() {
            FilterField alt1 = FilterField.builder().field("status").operation("eq").value("COMPLETE").build();
            FilterField alt2 = FilterField.builder().field("status").operation("eq").value("IN_PROGRESS").build();
            FilterField mainField = FilterField.builder()
                    .field("status").operation("eq").value("OPEN")
                    .alternatives(Arrays.asList(alt1, alt2)).build();

            assertNotNull(buildSpec(mainField));
            verify(cb).or(any(Predicate[].class));
        }
    }

    @Nested
    class FieldPathResolution {

        @Test
        void simpleField_shouldResolveDirectly() {
            assertNotNull(buildSpec(createFilterField("name", "eq", "test")));
            verify(root).get("name");
        }

        @Test
        void nestedFieldWithoutCollections_shouldResolveWithDotNotation() {
            Path nestedPath = mock(Path.class);
            lenient().doReturn((Class) String.class).when(nestedPath).getJavaType();
            when(root.get("details")).thenReturn(nestedPath);
            when(nestedPath.get("description")).thenReturn(simplePath);

            assertNotNull(buildSpec(createFilterField("details.description", "eq", "test")));
            verify(root).get("details");
            verify(nestedPath).get("description");
        }

        @Test
        void collectionField_shouldCreateJoin() {
            when(root.join("categories", JoinType.LEFT)).thenReturn(join);
            when(join.get("id")).thenReturn(entityIdPath);

            FilterField filterField = FilterField.builder()
                    .field("categories.id").operation("eq").value(1L).build();

            assertNotNull(buildSpec(filterField));
            verify(root).join("categories", JoinType.LEFT);
            verify(cb).equal(entityIdPath, 1L);
        }

        @Test
        void entityAssociationField_shouldUpdateCurrentFrom() {
            assertNotNull(buildSpec(createFilterField("owner.name", "eq", "test")));
            verify(root).get("owner");
            verify(fromPath).get("name");
        }
    }
}
