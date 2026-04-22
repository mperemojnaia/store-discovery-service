package com.jumbo.closeststores.controller;

import com.jumbo.closeststores.api.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturn400WithViolationDetails_whenConstraintViolationOccurs() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("latitude");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be between -90 and 90");

        ResponseEntity<ErrorResponse> response =
                handler.handleValidation(new ConstraintViolationException(Set.of(violation)));

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals(400, response.getBody().getStatus()),
                () -> assertTrue(response.getBody().getMessage().contains("latitude"))
        );
    }

    @Test
    void shouldReturn400WithParameterName_whenTypeMismatchOccurs() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("latitude");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertTrue(response.getBody().getMessage().contains("latitude")),
                () -> assertTrue(response.getBody().getMessage().contains("numeric"))
        );
    }

    @Test
    void shouldReturn400WithParamName_whenRequiredParamIsMissing() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("longitude", "Double");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParam(ex);

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertTrue(response.getBody().getMessage().contains("longitude")),
                () -> assertTrue(response.getBody().getMessage().contains("missing"))
        );
    }

    @Test
    void shouldReturn400WithExactMessage_whenIllegalArgumentThrown() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Latitude must be between -90 and 90"));

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("Latitude must be between -90 and 90", response.getBody().getMessage())
        );
    }

    @Test
    void shouldReturn500WithGenericMessage_whenUnexpectedExceptionOccurs() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneral(new RuntimeException("internal details"));

        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()),
                () -> assertFalse(response.getBody().getMessage().contains("internal details"),
                        "Should not leak internal error details")
        );
    }

    @Test
    void shouldIncludeValidIso8601Timestamp_inAllErrorResponses() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("test"));

        assertDoesNotThrow(() -> Instant.parse(response.getBody().getTimestamp()));
    }
}
