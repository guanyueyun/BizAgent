
package com.example.bizagent.config;

import com.example.bizagent.common.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleException(Exception e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseEntity.error(500, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @org.springframework.web.bind.annotation.RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseEntity.error(400, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @org.springframework.web.bind.annotation.RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseEntity.error(400, e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    @org.springframework.web.bind.annotation.RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<ResponseEntity<Void>> handleSecurityException(SecurityException e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseEntity.error(403, e.getMessage()));
    }
}
