package com.backendintranet.controller;

import com.backendintranet.exception.AbsenceConflictException;

import org.springframework.core.annotation.Order;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AbsenceController.class)
@Order(-1)
public class AbsenceExceptionHandler {
    @ExceptionHandler(AbsenceConflictException.class)
    public ResponseEntity<?> conflict(AbsenceConflictException e) {
        return ResponseEntity.status(409).body(Map.of("status", 409, "message", e.getMessage()));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<?> concurrentDecision() {
        return ResponseEntity.status(409)
                .body(
                        Map.of(
                                "status",
                                409,
                                "message",
                                "La solicitud está siendo procesada; vuelva a consultar su"
                                    + " estado."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> invalidBody() {
        return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "message", "JSON o formato de fecha/hora inválido."));
    }
}
