package com.grash.advancedsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SearchOperationTest {

    @ParameterizedTest
    @MethodSource("simpleOperationProvider")
    void getSimpleOperation_shouldMapCorrectly(String input, SearchOperation expected) {
        assertEquals(expected, SearchOperation.getSimpleOperation(input));
    }

    private static Stream<Arguments> simpleOperationProvider() {
        return Stream.of(
                Arguments.of("cn", SearchOperation.CONTAINS),
                Arguments.of("nc", SearchOperation.DOES_NOT_CONTAIN),
                Arguments.of("eq", SearchOperation.EQUAL),
                Arguments.of("ne", SearchOperation.NOT_EQUAL),
                Arguments.of("bw", SearchOperation.BEGINS_WITH),
                Arguments.of("bn", SearchOperation.DOES_NOT_BEGIN_WITH),
                Arguments.of("ew", SearchOperation.ENDS_WITH),
                Arguments.of("en", SearchOperation.DOES_NOT_END_WITH),
                Arguments.of("nu", SearchOperation.NUL),
                Arguments.of("nn", SearchOperation.NOT_NULL),
                Arguments.of("gt", SearchOperation.GREATER_THAN),
                Arguments.of("ge", SearchOperation.GREATER_THAN_EQUAL),
                Arguments.of("lt", SearchOperation.LESS_THAN),
                Arguments.of("le", SearchOperation.LESS_THAN_EQUAL),
                Arguments.of("in", SearchOperation.IN),
                Arguments.of("inm", SearchOperation.IN_MANY_TO_MANY)
        );
    }

    @Test
    void getSimpleOperation_shouldReturnNullForUnknownInput() {
        assertNull(SearchOperation.getSimpleOperation("unknown"));
        assertNull(SearchOperation.getSimpleOperation(""));
    }

    @Test
    void getDataOption_shouldReturnAll() {
        assertEquals(SearchOperation.ALL, SearchOperation.getDataOption("all"));
    }

    @Test
    void getDataOption_shouldReturnAny() {
        assertEquals(SearchOperation.ANY, SearchOperation.getDataOption("any"));
    }

    @Test
    void getDataOption_shouldReturnNullForUnknown() {
        assertNull(SearchOperation.getDataOption("unknown"));
        assertNull(SearchOperation.getDataOption(""));
    }

    @Test
    void enumValues_shouldContainAllExpectedConstants() {
        SearchOperation[] values = SearchOperation.values();
        assertTrue(values.length >= 15);
        assertNotNull(SearchOperation.valueOf("CONTAINS"));
        assertNotNull(SearchOperation.valueOf("EQUAL"));
        assertNotNull(SearchOperation.valueOf("IN"));
        assertNotNull(SearchOperation.valueOf("IN_MANY_TO_MANY"));
        assertNotNull(SearchOperation.valueOf("NUL"));
        assertNotNull(SearchOperation.valueOf("NOT_NULL"));
    }

    @Test
    void simpleOperationSet_shouldContainAllMappings() {
        assertEquals(16, SearchOperation.SIMPLE_OPERATION_SET.length);
    }
}
