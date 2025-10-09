package com.technicalchallenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex){
        boolean missingBook = ex.getBindingResult().hasFieldErrors("bookName");
        boolean missingCounterparty = ex.getBindingResult().hasFieldErrors("counterpartyName");

        if(missingBook || missingCounterparty){
            return ResponseEntity.badRequest().body("Book and Counterparty are required");
        }

        String firstError = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(firstError);
    }
}
