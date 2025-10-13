package com.kevin.financetracker.exception;

import com.kevin.financetracker.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Handle Business Logic Exceptions
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Business Rule Violation",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Handle Duplicate Resource
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Duplicate Resource",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Handle Validation Exceptions
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Handle Spring Validation Errors (@Valid on Request Body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // Collect all field errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields failed validation",
                request.getRequestURI(),
                LocalDateTime.now(),
                fieldErrors  // Include field-level errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Handle Constraint Violation Exception (@Validated on method parameters)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = getFieldNameFromPath(violation.getPropertyPath().toString());
            violations.put(fieldName, violation.getMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation",
                "One or more parameters failed validation",
                request.getRequestURI(),
                LocalDateTime.now(),
                violations
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Handle Illegal Argument Exceptions
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Handle Illegal State Exceptions
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Invalid State",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Handle All Other Exceptions (Fallback)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        // Log the exception for debugging (consider using a proper logger)
        ex.printStackTrace();

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Invalid JSON format: " + ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Helper method to extract field name from property path
    private String getFieldNameFromPath(String propertyPath) {
        // Property path format: methodName.parameterName.fieldName
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }
}