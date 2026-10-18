package com.example.menu;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Map;

/** Every failure is a JSON {"error": "..."} body; the app never leaks a stack trace or HTML page. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, String>> json(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, String>> malformedJson(JsonProcessingException e) {
        return json(HttpStatus.BAD_REQUEST, "Malformed JSON");
    }

    // Unknown routes and unsupported methods on known routes -> JSON 404
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Map<String, String>> routeNotFound(Exception e) {
        return json(HttpStatus.NOT_FOUND, "Route not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> fallback(Exception e) {
        return json(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
