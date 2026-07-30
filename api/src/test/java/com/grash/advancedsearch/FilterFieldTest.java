package com.grash.advancedsearch;

import jakarta.persistence.criteria.JoinType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterFieldTest {

    @Test
    void builder_shouldCreateFilterFieldWithAllFields() {
        List<Object> values = Arrays.asList("val1", "val2");
        FilterField field = FilterField.builder()
                .field("name")
                .joinType(JoinType.INNER)
                .value("testValue")
                .operation("eq")
                .values(values)
                .build();

        assertEquals("name", field.getField());
        assertEquals(JoinType.INNER, field.getJoinType());
        assertEquals("testValue", field.getValue());
        assertEquals("eq", field.getOperation());
        assertEquals(values, field.getValues());
    }

    @Test
    void noArgsConstructor_shouldCreateEmptyFilterField() {
        FilterField field = new FilterField();
        assertNull(field.getField());
        assertNull(field.getJoinType());
        assertNull(field.getValue());
        assertNull(field.getOperation());
        assertNotNull(field.getValues());
        assertTrue(field.getValues().isEmpty());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        List<Object> values = new ArrayList<>();
        FilterField field = new FilterField("status", JoinType.LEFT, "OPEN",
                "eq", values, null, null);

        assertEquals("status", field.getField());
        assertEquals(JoinType.LEFT, field.getJoinType());
        assertEquals("OPEN", field.getValue());
        assertEquals("eq", field.getOperation());
        assertSame(values, field.getValues());
        assertNull(field.getAlternatives());
        assertNull(field.getEnumName());
    }

    @Test
    void setters_shouldUpdateFields() {
        FilterField field = new FilterField();
        field.setField("priority");
        field.setOperation("gt");
        field.setValue(3);
        field.setJoinType(JoinType.INNER);
        List<Object> values = List.of("a", "b");
        field.setValues(values);

        assertEquals("priority", field.getField());
        assertEquals("gt", field.getOperation());
        assertEquals(3, field.getValue());
        assertEquals(JoinType.INNER, field.getJoinType());
        assertEquals(values, field.getValues());
    }

    @Test
    void equalsAndHashCode_shouldWorkForSameValues() {
        FilterField f1 = FilterField.builder().field("name").operation("eq").value("test").build();
        FilterField f2 = FilterField.builder().field("name").operation("eq").value("test").build();
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    void equals_shouldReturnFalseForDifferentFields() {
        FilterField f1 = FilterField.builder().field("name").operation("eq").build();
        FilterField f2 = FilterField.builder().field("status").operation("eq").build();
        assertNotEquals(f1, f2);
    }

    @Test
    void toString_shouldContainFieldAndOperation() {
        FilterField field = FilterField.builder().field("name").operation("eq").value("test").build();
        String str = field.toString();
        assertTrue(str.contains("name"));
        assertTrue(str.contains("eq"));
    }
}
