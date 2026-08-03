package com.grash.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void constructor_retainsMessage() {
        CustomException ex = new CustomException("Resource not found", HttpStatus.NOT_FOUND);

        assertEquals("Resource not found", ex.getMessage());
    }

    @Test
    void constructor_retainsHttpStatus() {
        CustomException ex = new CustomException("Unauthorized", HttpStatus.UNAUTHORIZED);

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void isRuntimeException() {
        CustomException ex = new CustomException("Oops", HttpStatus.BAD_REQUEST);

        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void message_isNotOverriddenByThrowable() {
        Throwable cause = new RuntimeException("root cause");
        CustomException ex = new CustomException("Wrapped message", HttpStatus.CONFLICT);

        assertNotEquals(cause.getMessage(), ex.getMessage());
        assertEquals("Wrapped message", ex.getMessage());
    }
}
