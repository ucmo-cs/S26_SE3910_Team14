package com.bankscheduling.appointment.config.web;

import com.bankscheduling.appointment.dto.auth.AuthStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<AuthStatusResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new AuthStatusResponse("error", ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString()));
    }
}
