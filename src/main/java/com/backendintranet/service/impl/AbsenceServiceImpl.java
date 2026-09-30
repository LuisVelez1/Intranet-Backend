package com.backendintranet.service.impl;

import com.backendintranet.dto.request.AbsenceCreateRequest;
import com.backendintranet.dto.request.AbsenceDecisionRequest;
import com.backendintranet.dto.response.AbsenceResponse;
import com.backendintranet.entity.AbsenceMail;
import com.backendintranet.entity.AbsenceRequest;
import com.backendintranet.entity.AbsenceStatus;
import com.backendintranet.entity.User;
import com.backendintranet.exception.AbsenceConflictException;
import com.backendintranet.exception.BadRequestException;
import com.backendintranet.exception.ResourceNotFoundException;
import com.backendintranet.repository.AbsenceApproverAssignmentRepository;
import com.backendintranet.repository.AbsenceMailRepository;
import com.backendintranet.repository.AbsenceRequestRepository;
import com.backendintranet.repository.AbsenceTypeRepository;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.AbsenceService;

import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRequestRepository requests;
    private final AbsenceApproverAssignmentRepository assignments;
    private final AbsenceTypeRepository absenceTypes;
    private final UserRepository users;
    private final AbsenceMailRepository mail;
    private final Validator validator;

    @Value("${absence.mail.hr-recipient:}")
    private String hrRecipient;

    private User currentUser() {

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                        instanceof AnonymousAuthenticationToken) {

            throw new AccessDeniedException(
                    "Autenticación requerida");
        }

        User user =
                users.findByUsername(
                                authentication.getName())
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Usuario no disponible"));

        if (!user.isEnabled()) {
            throw new AccessDeniedException(
                    "Usuario inactivo");
        }

        return user;
    }

    private void validate(Object value) {

        if (value == null
                || !validator
                        .validate(value)
                        .isEmpty()) {

            throw new BadRequestException(
                    "Datos de ausencia inválidos");
        }
    }

    /**
     * Comprueba roles de forma segura.
     *
     * Algunos tests o usuarios pueden no tener la colección
     * de roles inicializada, por lo que evitamos NPE.
     */
    private boolean hasRole(
            User user,
            String role) {

        if (user == null
                || user.getRoles() == null) {

            return false;
        }

        return user.getRoles()
                .stream()
                .anyMatch(
                        r ->
                                r != null
                                        && r.getName() != null
                                        && r.getName()
                                                .equalsIgnoreCase(role));
    }

    private boolean isTalentHuman(
            User user) {

        return hasRole(
                user,
                "TALENTO_HUMANO");
    }

    private boolean isSuperAdmin(
            User user) {

        return hasRole(
                user,
                "SUPER_ADMIN");
    }

    /**
     * Talento Humano y Super Admin tienen acceso
     * global al módulo de ausentismo.
     */
    private boolean isGlobalAbsenceManager(
            User user) {

        return isTalentHuman(user)
                || isSuperAdmin(user);
    }

    /**
     * Determina si el usuario es actualmente jefe
     * de al menos un empleado.
     *
     * Se utiliza para informar capacidades al frontend.
     */
    private boolean isApprover(
            User user) {

        return assignments
                .existsByApprover_IdAndActiveTrue(
                        user.getId());
    }

    @Override
    public AbsenceResponse create(
            AbsenceCreateRequest input) {

        User requester =
                currentUser();

        validate(input);

        var absenceType =
                absenceTypes
                        .findByNameIgnoreCaseAndActiveTrue(
                                input.getType().trim())
                        .orElseThrow(
                                () ->
                                        new BadRequestException(
                                                "El tipo de ausencia no existe o se encuentra inactivo"));

        User approver =
                assignments
                        .findByEmployee_IdAndActiveTrue(
                                requester.getId())
                        .orElseThrow(
                                () ->
                                        new BadRequestException(
                                                "El empleado no tiene aprobador activo configurado"))
                        .getApprover();

        if (!approver.isEnabled()
                || requester
                        .getId()
                        .equals(
                                approver.getId())) {

            throw new BadRequestException(
                    "El aprobador debe estar activo y ser distinto del solicitante");
        }

        AbsenceRequest request =
                new AbsenceRequest();

        request.setRequester(
                requester);

        request.setApprover(
                approver);

        request.setType(
                absenceType.getName());

        request.setStartDate(
                input.getStartDate());

        request.setEndDate(
                input.getEndDate());

        request.setStartTime(
                input.getStartTime());

        request.setEndTime(
                input.getEndTime());

        request.setReason(
                input.getReason().trim());

        request.setStatus(
                AbsenceStatus.PENDING);

        request =
                requests.saveAndFlush(
                        request);

        /*
         * Notificación al jefe inmediato.
         */
        enqueue(
                approver,
                request,
                "Solicitud de ausencia pendiente",
                "Tiene una solicitud pendiente de "
                        + name(requester)
                        + ".");

        /*
         * Notificación a Talento Humano.
         *
         * El destinatario viene desde:
         * ABSENCE_MAIL_HR_RECIPIENT
         */
        enqueueEmail(
                hrRecipient,
                request,
                "Nueva solicitud de ausencia",
                name(requester)
                        + " ha registrado una nueva solicitud de ausencia.");

        return response(
                request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceResponse> findMyRequests() {

        return requests
                .findByRequester_IdOrderByCreatedAtDesc(
                        currentUser().getId())
                .stream()
                .map(this::response)
                .toList();
    }

    /**
     * Comportamiento:
     *
     * TALENTO_HUMANO / SUPER_ADMIN:
     *      todas las solicitudes pendientes.
     *
     * Jefe inmediato:
     *      únicamente las solicitudes asignadas a él.
     *
     * Usuario normal:
     *      lista vacía.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AbsenceResponse> findPendingApprovals() {

        User actor =
                currentUser();

        if (isGlobalAbsenceManager(actor)) {

            return requests
                    .findByStatusOrderByCreatedAtAsc(
                            AbsenceStatus.PENDING_HR)
                    .stream()
                    .map(this::response)
                    .toList();
        }

        return requests
                .findByApprover_IdAndStatusOrderByCreatedAtAsc(
                        actor.getId(),
                        AbsenceStatus.PENDING)
                .stream()
                .map(this::response)
                .toList();
    }

    /**
     * Información corporativa de ausentismo.
     *
     * Solo:
     * TALENTO_HUMANO
     * SUPER_ADMIN
     */
    @Override
    @Transactional(readOnly = true)
    public List<AbsenceResponse> findAllForReporting() {

        User actor =
                currentUser();

        if (!isGlobalAbsenceManager(
                actor)) {

            throw new AccessDeniedException(
                    "No tiene permiso para consultar indicadores globales");
        }

        return requests
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::response)
                .toList();
    }

    /**
     * Informa al frontend qué opciones debe mostrar.
     *
     * Esto evita que Angular tenga que inferir
     * quién es jefe únicamente mirando roles.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> getCapabilities() {

        User actor =
                currentUser();

        boolean globalManager =
                isGlobalAbsenceManager(
                        actor);

        boolean approver =
                isApprover(actor);

        return Map.of(
                "canApprove",
                globalManager || approver,

                "canViewIndicators",
                globalManager,

                "canManageAbsenceTypes",
                globalManager);
    }

    @Override
    public AbsenceResponse approve(
            String id,
            AbsenceDecisionRequest input) {

        return decide(
                id,
                input,
                AbsenceStatus.APPROVED);
    }

    @Override
    public AbsenceResponse reject(
            String id,
            AbsenceDecisionRequest input) {

        return decide(
                id,
                input,
                AbsenceStatus.REJECTED);
    }

    /**
     * Flujo de doble aprobación:
     *
     * PENDING
     *      -> decide únicamente el jefe inmediato.
     *
     * Si el jefe aprueba:
     *      PENDING -> PENDING_HR
     *
     * Si el jefe rechaza:
     *      PENDING -> REJECTED
     *
     * PENDING_HR
     *      -> decide únicamente Talento Humano
     *         o Super Admin.
     *
     * Si TH aprueba:
     *      PENDING_HR -> APPROVED
     *
     * Si TH rechaza:
     *      PENDING_HR -> REJECTED
     */
    private AbsenceResponse decide(
            String id,
            AbsenceDecisionRequest input,
            AbsenceStatus decision) {

        User actor =
                currentUser();

        validate(input);

        AbsenceRequest request =
                requests
                        .findForDecision(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Solicitud no encontrada"));

        String comment =
                input.getComment() == null
                        ? null
                        : input.getComment().trim();

        /*
         * =================================================
         * ETAPA 1 - JEFE INMEDIATO
         * =================================================
         */
        if (request.getStatus()
                == AbsenceStatus.PENDING) {

            boolean assignedApprover =
                    request
                            .getApprover()
                            .getId()
                            .equals(
                                    actor.getId());

            if (!assignedApprover) {

                throw new AccessDeniedException(
                        "La solicitud está pendiente de decisión del jefe inmediato");
            }

            request.setBossDecision(
                    decision.name());

            request.setBossDecisionAt(
                    LocalDateTime.now());

            request.setBossComment(
                    comment);

            /*
             * El jefe rechaza:
             * la solicitud termina inmediatamente.
             */
            if (decision
                    == AbsenceStatus.REJECTED) {

                request.setStatus(
                        AbsenceStatus.REJECTED);

                /*
                 * Compatibilidad con clientes actuales:
                 * approvalComment representa la decisión
                 * final visible.
                 */
                request.setApprovalComment(
                        comment);

                request.setApprovedAt(
                        null);

                request =
                        requests.saveAndFlush(
                                request);

                enqueue(
                        request.getRequester(),
                        request,
                        "Decisión de solicitud de ausencia",
                        "Su solicitud fue rechazada por su jefe inmediato.");

                return response(
                        request);
            }

            /*
             * El jefe aprueba:
             * pasa a revisión de Talento Humano.
             */
            request.setStatus(
                    AbsenceStatus.PENDING_HR);

            request =
                    requests.saveAndFlush(
                            request);

            enqueueEmail(
                    hrRecipient,
                    request,
                    "Solicitud de ausencia pendiente de aprobación final",
                    name(request.getRequester())
                            + " tiene una solicitud aprobada por su jefe inmediato y pendiente de revisión por Talento Humano.");

            return response(
                    request);
        }

        /*
         * =================================================
         * ETAPA 2 - TALENTO HUMANO
         * =================================================
         */
        if (request.getStatus()
                == AbsenceStatus.PENDING_HR) {

            boolean alreadyDecidedByBoss =
                    request
                            .getApprover()
                            .getId()
                            .equals(
                                    actor.getId());

            if (alreadyDecidedByBoss) {

                throw new AbsenceConflictException(
                        "El jefe inmediato ya decidió esta solicitud");
            }

            if (!isGlobalAbsenceManager(
                    actor)) {

                throw new AccessDeniedException(
                        "La solicitud está pendiente de decisión de Talento Humano");
            }

            request.setHrDecision(
                    decision.name());

            request.setHrDecisionAt(
                    LocalDateTime.now());

            request.setHrComment(
                    comment);

            /*
             * approvalComment se conserva para
             * compatibilidad con el frontend actual.
             */
            request.setApprovalComment(
                    comment);

            if (decision
                    == AbsenceStatus.APPROVED) {

                request.setStatus(
                        AbsenceStatus.APPROVED);

                request.setApprovedAt(
                        LocalDateTime.now());

            } else {

                request.setStatus(
                        AbsenceStatus.REJECTED);

                request.setApprovedAt(
                        null);
            }

            request =
                    requests.saveAndFlush(
                            request);

            enqueue(
                    request.getRequester(),
                    request,
                    "Decisión final de solicitud de ausencia",
                    "Su solicitud fue "
                            + (decision
                                            == AbsenceStatus.APPROVED
                                    ? "aprobada"
                                    : "rechazada")
                            + " por Talento Humano.");

            return response(
                    request);
        }

        /*
         * APPROVED / REJECTED / CANCELLED
         * ya son estados terminales.
         */
        throw new AbsenceConflictException(
                "La solicitud ya no está pendiente");
    }

    /**
     * Encola correo utilizando el usuario interno.
     */
    private void enqueue(
            User recipient,
            AbsenceRequest request,
            String subject,
            String text) {

        if (recipient.getEmail() == null
                || recipient
                        .getEmail()
                        .isBlank()) {

            throw new BadRequestException(
                    "El destinatario no tiene correo configurado");
        }

        enqueueEmail(
                recipient.getEmail(),
                request,
                subject,
                text);
    }

    /**
     * Encola correo utilizando directamente
     * una dirección de email.
     *
     * Importante:
     * el motivo NO se incluye en el correo.
     *
     * El detalle completo debe consultarse
     * autenticándose en la intranet.
     */
    private void enqueueEmail(
            String recipient,
            AbsenceRequest request,
            String subject,
            String text) {

        if (recipient == null
                || recipient.isBlank()) {

            return;
        }

        AbsenceMail message =
                new AbsenceMail();

        message.setRecipient(
                recipient.trim());

        message.setSubject(
                subject);

        message.setBody(
                text
                        + "\nSolicitud: "
                        + request.getId()
                        + "\nTipo: "
                        + request.getType()
                        + "\nPeriodo: "
                        + request.getStartDate()
                        + " al "
                        + request.getEndDate()
                        + "\nHora de salida: "
                        + request.getStartTime()
                        + "\nHora de regreso: "
                        + request.getEndTime()
                        + "\nConsulte los detalles en la intranet.");

        mail.save(
                message);
    }

    private String name(
            User user) {

        return user.getFirstName()
                + " "
                + user.getLastName();
    }

    private AbsenceResponse response(
            AbsenceRequest request) {

        return AbsenceResponse
                .builder()
                .id(
                        request.getId())

                .requesterId(
                        request
                                .getRequester()
                                .getId())

                .requesterName(
                        name(
                                request
                                        .getRequester()))

                .approverId(
                        request
                                .getApprover()
                                .getId())

                .approverName(
                        name(
                                request
                                        .getApprover()))

                .type(
                        request.getType())

                .startDate(
                        request.getStartDate())

                .endDate(
                        request.getEndDate())

                .startTime(
                        request.getStartTime())

                .endTime(
                        request.getEndTime())

                .reason(
                        request.getReason())

                .supportFile(
                        request.getSupportFile())

                .status(
                        request.getStatus())

                .createdAt(
                        request.getCreatedAt())

                .updatedAt(
                        request.getUpdatedAt())

                .approvedAt(
                        request.getApprovedAt())

                .approvalComment(
                        request.getApprovalComment())

                .bossDecision(
                        request.getBossDecision())

                .bossDecisionAt(
                        request.getBossDecisionAt())

                .bossComment(
                        request.getBossComment())

                .hrDecision(
                        request.getHrDecision())

                .hrDecisionAt(
                        request.getHrDecisionAt())

                .hrComment(
                        request.getHrComment())

                .build();
    }
}
