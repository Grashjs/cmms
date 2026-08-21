package com.grash.exception;

import com.grash.dto.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerControllerTest {

    @Mock
    private HttpServletResponse response;

    private GlobalExceptionHandlerController handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandlerController();
    }

    @Test
    void handleCustomException_returnsHttpStatusAndMessage() {
        CustomException ex = new CustomException("Part not found", HttpStatus.NOT_FOUND);

        ResponseEntity<SuccessResponse> result = handler.handleCustomException(response, ex);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals("Part not found", result.getBody().getMessage());
    }

    @Test
    void handleAccessDeniedException_returnsForbidden() {
        ResponseEntity<SuccessResponse> result = handler.handleAccessDeniedException(response);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals("Access is denied", result.getBody().getMessage());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<SuccessResponse> result =
                handler.handleHttpRequestMethodNotSupportedException(response, ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertTrue(result.getBody().getMessage().contains("POST"));
    }

    @Test
    void handleValidationException_returnsBadRequest() throws Exception {
        ValidationException ex = new ValidationException("Invalid payload");

        ResponseEntity<SuccessResponse> result = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals("Invalid payload", result.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValidException_returnsFieldErrorMessage() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userSignupRequest");
        bindingResult.addError(new FieldError("userSignupRequest", "password",
                "Password is too common. Please choose a more unique password"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<SuccessResponse> result = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals("Password is too common. Please choose a more unique password", result.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValidException_joinsMessagesAndSkipsNulls() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userSignupRequest");
        bindingResult.addError(new FieldError("userSignupRequest", "password", null));
        bindingResult.addError(new FieldError("userSignupRequest", "email", "Email must be valid"));
        bindingResult.addError(new FieldError("userSignupRequest", "firstName", "First name is required"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<SuccessResponse> result = handler.handleMethodArgumentNotValidException(ex);

        assertEquals("Email must be valid, First name is required", result.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValidException_fallsBackWhenNoMessage() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userSignupRequest");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<SuccessResponse> result = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Validation failed", result.getBody().getMessage());
    }

    @Test
    void handleException_returnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected failure");

        ResponseEntity<SuccessResponse> result = handler.handleException(response, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isSuccess());
        assertEquals("Unexpected failure", result.getBody().getMessage());
    }
}
