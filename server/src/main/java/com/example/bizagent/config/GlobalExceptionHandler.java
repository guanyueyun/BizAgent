
package com.example.bizagent.config;

import com.example.bizagent.common.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleException(Exception e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseEntity.error(500, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseEntity.error(400, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseEntity.error(400, e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleSecurityException(SecurityException e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseEntity.error(403, e.getMessage()));
    }
}
