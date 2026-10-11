package com.backendintranet.controller;

import com.backendintranet.dto.request.AbsenceApproverAssignmentRequest;
import com.backendintranet.dto.response.AbsenceApproverAssignmentResponse;
import com.backendintranet.dto.response.AbsenceApproverUserResponse;
import com.backendintranet.service.AbsenceApproverAssignmentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/absence-approver-assignments")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyRole('TALENTO_HUMANO','SUPER_ADMIN')")
public class AbsenceApproverAssignmentController {

    private final AbsenceApproverAssignmentService service;

    @GetMapping
    public List<AbsenceApproverAssignmentResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/users")
    public List<AbsenceApproverUserResponse> findUsers() {
        return service.findActiveUsers();
    }

    @PutMapping("/{employeeId}")
    public AbsenceApproverAssignmentResponse assign(
            @PathVariable String employeeId,
            @Valid
            @RequestBody
            AbsenceApproverAssignmentRequest request) {

        return service.assign(
                employeeId,
                request);
    }

    @PutMapping("/{employeeId}/deactivate")
    public AbsenceApproverAssignmentResponse deactivate(
            @PathVariable String employeeId) {

        return service.deactivate(
                employeeId);
    }
}
