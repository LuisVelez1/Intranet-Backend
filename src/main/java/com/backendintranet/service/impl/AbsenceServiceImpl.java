package com.backendintranet.service.impl;

import com.backendintranet.dto.request.*;
import com.backendintranet.dto.response.AbsenceResponse;
import com.backendintranet.entity.*;
import com.backendintranet.exception.*;
import com.backendintranet.repository.*;
import com.backendintranet.service.AbsenceService;

import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AbsenceServiceImpl implements AbsenceService {
    private final AbsenceRequestRepository requests;
    private final AbsenceApproverAssignmentRepository assignments;
    private final UserRepository users;
    private final AbsenceMailRepository mail;
    private final Validator validator;

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken)
            throw new AccessDeniedException("Autenticación requerida");
        User user =
                users.findByUsername(authentication.getName())
                        .orElseThrow(() -> new AccessDeniedException("Usuario no disponible"));
        if (!user.isEnabled()) throw new AccessDeniedException("Usuario inactivo");
        return user;
    }

    private void validate(Object value) {
        if (value == null || !validator.validate(value).isEmpty())
            throw new BadRequestException("Datos de ausencia inválidos");
    }

    @Override
    public AbsenceResponse create(AbsenceCreateRequest input) {
        User requester = currentUser();
        validate(input);
        User approver =
                assignments
                        .findByEmployee_IdAndActiveTrue(requester.getId())
                        .orElseThrow(
                                () ->
                                        new BadRequestException(
                                                "El empleado no tiene aprobador activo"
                                                    + " configurado"))
                        .getApprover();
        if (!approver.isEnabled() || requester.getId().equals(approver.getId()))
            throw new BadRequestException(
                    "El aprobador debe estar activo y ser distinto del solicitante");
        AbsenceRequest r = new AbsenceRequest();
        r.setRequester(requester);
        r.setApprover(approver);
        r.setType(input.getType().trim());
        r.setStartDate(input.getStartDate());
        r.setEndDate(input.getEndDate());
        r.setStartTime(input.getStartTime());
        r.setEndTime(input.getEndTime());
        r.setReason(input.getReason().trim());
        r.setStatus(AbsenceStatus.PENDING);
        r = requests.saveAndFlush(r);
        enqueue(
                approver,
                r,
                "Solicitud de ausencia pendiente",
                "Tiene una solicitud pendiente de " + name(requester) + ".");
        return response(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceResponse> findMyRequests() {
        return requests.findByRequester_IdOrderByCreatedAtDesc(currentUser().getId()).stream()
                .map(this::response)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceResponse> findPendingApprovals() {
        return requests
                .findByApprover_IdAndStatusOrderByCreatedAtAsc(
                        currentUser().getId(), AbsenceStatus.PENDING)
                .stream()
                .map(this::response)
                .toList();
    }

    @Override
    public AbsenceResponse approve(String id, AbsenceDecisionRequest input) {
        return decide(id, input, AbsenceStatus.APPROVED);
    }

    @Override
    public AbsenceResponse reject(String id, AbsenceDecisionRequest input) {
        return decide(id, input, AbsenceStatus.REJECTED);
    }

    private AbsenceResponse decide(String id, AbsenceDecisionRequest input, AbsenceStatus status) {
        User actor = currentUser();
        validate(input);
        AbsenceRequest r =
                requests.findForDecision(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!r.getApprover().getId().equals(actor.getId()))
            throw new AccessDeniedException(
                    "Solo el aprobador asignado puede decidir esta solicitud");
        if (r.getStatus() != AbsenceStatus.PENDING)
            throw new AbsenceConflictException("La solicitud ya no está pendiente");
        r.setStatus(status);
        r.setApprovalComment(input.getComment() == null ? null : input.getComment().trim());
        if (status == AbsenceStatus.APPROVED) r.setApprovedAt(LocalDateTime.now());
        r = requests.saveAndFlush(r);
        enqueue(
                r.getRequester(),
                r,
                "Decisión de solicitud de ausencia",
                "Su solicitud fue "
                        + (status == AbsenceStatus.APPROVED ? "aprobada" : "rechazada")
                        + ".");
        return response(r);
    }

    private void enqueue(User recipient, AbsenceRequest r, String subject, String text) {
        if (recipient.getEmail() == null || recipient.getEmail().isBlank())
            throw new BadRequestException("El destinatario no tiene correo configurado");
        AbsenceMail message = new AbsenceMail();
        message.setRecipient(recipient.getEmail());
        message.setSubject(subject);
        message.setBody(
                text
                        + "\nSolicitud: "
                        + r.getId()
                        + "\nPeriodo: "
                        + r.getStartDate()
                        + " al "
                        + r.getEndDate()
                        + "\nConsulte los detalles en la intranet.");
        mail.save(message);
    }

    private String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }

    private AbsenceResponse response(AbsenceRequest r) {
        return AbsenceResponse.builder()
                .id(r.getId())
                .requesterId(r.getRequester().getId())
                .requesterName(name(r.getRequester()))
                .approverId(r.getApprover().getId())
                .approverName(name(r.getApprover()))
                .type(r.getType())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .reason(r.getReason())
                .supportFile(r.getSupportFile())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .approvedAt(r.getApprovedAt())
                .approvalComment(r.getApprovalComment())
                .build();
    }
}
