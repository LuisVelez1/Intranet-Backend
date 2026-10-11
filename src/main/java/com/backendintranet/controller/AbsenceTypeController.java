package com.backendintranet.controller;

import com.backendintranet.dto.request.AbsenceTypeRequest;
import com.backendintranet.dto.response.AbsenceTypeResponse;
import com.backendintranet.service.AbsenceTypeService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/absence-types")
@RequiredArgsConstructor
public class AbsenceTypeController {

    private final AbsenceTypeService service;

    @GetMapping
    public List<AbsenceTypeResponse> active() {
        return service.findActive();
    }

    @GetMapping("/admin")
    @PreAuthorize(
            "hasAnyRole('TALENTO_HUMANO','SUPER_ADMIN')")
    public List<AbsenceTypeResponse> all() {
        return service.findAll();
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('TALENTO_HUMANO','SUPER_ADMIN')")
    public ResponseEntity<AbsenceTypeResponse> create(
            @Valid
            @RequestBody
            AbsenceTypeRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('TALENTO_HUMANO','SUPER_ADMIN')")
    public AbsenceTypeResponse update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            AbsenceTypeRequest request) {

        return service.update(
                id,
                request);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize(
            "hasAnyRole('TALENTO_HUMANO','SUPER_ADMIN')")
    public AbsenceTypeResponse deactivate(
            @PathVariable Long id) {

        return service.deactivate(id);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize(
            "hasAnyRole('TALENTO_HUMANO','SUPER_ADMIN')")
    public AbsenceTypeResponse activate(
            @PathVariable Long id) {

        return service.activate(id);
    }
}
