package com.backendintranet.service.impl;

import com.backendintranet.dto.request.AbsenceApproverAssignmentRequest;
import com.backendintranet.dto.response.AbsenceApproverAssignmentResponse;
import com.backendintranet.dto.response.AbsenceApproverUserResponse;
import com.backendintranet.entity.AbsenceApproverAssignment;
import com.backendintranet.entity.User;
import com.backendintranet.exception.BadRequestException;
import com.backendintranet.exception.ResourceNotFoundException;
import com.backendintranet.repository.AbsenceApproverAssignmentRepository;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.AbsenceApproverAssignmentService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AbsenceApproverAssignmentServiceImpl
        implements AbsenceApproverAssignmentService {

    private final AbsenceApproverAssignmentRepository assignments;
    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceApproverAssignmentResponse> findAll() {

        return assignments.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                item -> name(item.getEmployee()),
                                String.CASE_INSENSITIVE_ORDER))
                .map(this::response)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceApproverUserResponse> findActiveUsers() {

        return users.findAll()
                .stream()
                .filter(User::isEnabled)
                .sorted(
                        Comparator.comparing(
                                this::name,
                                String.CASE_INSENSITIVE_ORDER))
                .map(this::userResponse)
                .toList();
    }

    @Override
    public AbsenceApproverAssignmentResponse assign(
            String employeeId,
            AbsenceApproverAssignmentRequest request) {

        User employee =
                users.findById(employeeId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Empleado no encontrado"));

        User approver =
                users.findById(request.getApproverId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Aprobador no encontrado"));

        if (!employee.isEnabled()) {
            throw new BadRequestException(
                    "El empleado debe estar activo");
        }

        if (!approver.isEnabled()) {
            throw new BadRequestException(
                    "El aprobador debe estar activo");
        }

        if (employee.getId().equals(approver.getId())) {
            throw new BadRequestException(
                    "El empleado no puede ser su propio aprobador");
        }

        AbsenceApproverAssignment assignment =
                assignments
                        .findByEmployee_Id(employeeId)
                        .orElseGet(AbsenceApproverAssignment::new);

        assignment.setEmployee(employee);
        assignment.setApprover(approver);
        assignment.setActive(true);

        return response(
                assignments.saveAndFlush(assignment));
    }

    @Override
    public AbsenceApproverAssignmentResponse deactivate(
            String employeeId) {

        AbsenceApproverAssignment assignment =
                assignments
                        .findByEmployee_Id(employeeId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Asignación de jefe no encontrada"));

        assignment.setActive(false);

        return response(
                assignments.saveAndFlush(assignment));
    }

    private AbsenceApproverAssignmentResponse response(
            AbsenceApproverAssignment assignment) {

        return AbsenceApproverAssignmentResponse
                .builder()
                .id(assignment.getId())
                .employeeId(
                        assignment.getEmployee().getId())
                .employeeName(
                        name(assignment.getEmployee()))
                .employeeEmail(
                        assignment.getEmployee().getEmail())
                .approverId(
                        assignment.getApprover().getId())
                .approverName(
                        name(assignment.getApprover()))
                .approverEmail(
                        assignment.getApprover().getEmail())
                .active(
                        assignment.getActive())
                .build();
    }

    private AbsenceApproverUserResponse userResponse(
            User user) {

        return AbsenceApproverUserResponse
                .builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(name(user))
                .email(user.getEmail())
                .position(user.getPosition())
                .sede(user.getSede())
                .build();
    }

    private String name(
            User user) {

        return (
                user.getFirstName()
                        + " "
                        + user.getLastName()
        ).trim();
    }
}
