package com.technicalchallenge.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import cz.jirutka.rsql.parser.RSQLParserException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex){
        boolean missingBook = ex.getBindingResult().hasFieldErrors("bookName");
        boolean missingCounterparty = ex.getBindingResult().hasFieldErrors("counterpartyName");

        if(missingBook || missingCounterparty){
            return ResponseEntity.badRequest().body("Book and Counterparty are required");
        }

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            errors.put("error", "Bad Request");
            String errorMessage = error.getDefaultMessage();
            errors.put("message", errorMessage);
            errors.put("timestamp", LocalDateTime.now().toString());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
  

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex){
        Map<String, String> response = Map.of(
            "error", "Bad Request",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RSQLParserException.class)
    public ResponseEntity<Map<String, String>> handleRSQLParserException(RSQLParserException ex){
        Map<String, String> response = Map.of(
            "error", "Bad Request",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedAccessException(UnauthorizedAccessException ex){
        Map<String, String> response = Map.of(
            "error", "Unauthorized operation",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    } 
}
