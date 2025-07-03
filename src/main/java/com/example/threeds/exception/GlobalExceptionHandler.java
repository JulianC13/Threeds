package com.example.threeds.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Global exception handler for the 3DS Server application.
 * Provides centralized error handling and consistent error response formatting
 * for various types of exceptions that may occur during request processing.
 * 
 * @author Julian Camilo
 * @version 1.0
 * @since 2025
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles CardRangeNotFoundException by returning a 404 Not Found response.
     * 
     * @param e       the CardRangeNotFoundException that was thrown
     * @param request the HTTP request that caused the exception
     * @return ResponseEntity with error details and HTTP 404 status
     */
    @ExceptionHandler(CardRangeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCardRangeNotFound(CardRangeNotFoundException e,
            jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now().toString());
        errorBody.put("status", HttpStatus.NOT_FOUND.value());
        errorBody.put("error", "Not Found");
        errorBody.put("message", e.getMessage());
        errorBody.put("path", request.getRequestURI());
        return new ResponseEntity<>(errorBody, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles NoHandlerFoundException by returning a 404 Not Found response.
     * 
     * @param ex the NoHandlerFoundException that was thrown
     * @return ResponseEntity with error details and HTTP 404 status
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 404);
        error.put("error", "Not Found");
        error.put("message", "The requested path does not exist.");
        error.put("path", ex.getRequestURL());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles HttpRequestMethodNotSupportedException by returning a 405 Method Not Allowed response.
     * 
     * @param ex the HttpRequestMethodNotSupportedException that was thrown
     * @return ResponseEntity with error details and HTTP 405 status
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 405);
        error.put("error", "Method Not Allowed");
        error.put("message", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Handles HttpMessageNotReadableException by returning a 400 Bad Request response.
     * This typically occurs when JSON deserialization fails due to malformed request body.
     * 
     * @param ex the HttpMessageNotReadableException that was thrown
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleDeserializationError(HttpMessageNotReadableException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 400);
        error.put("error", "Bad Request");
        error.put("message", ex.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}