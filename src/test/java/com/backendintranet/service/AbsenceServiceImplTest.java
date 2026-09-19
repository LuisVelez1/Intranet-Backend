package com.backendintranet.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.backendintranet.dto.request.*;
import com.backendintranet.entity.*;
import com.backendintranet.exception.*;
import com.backendintranet.repository.*;
import com.backendintranet.service.impl.AbsenceServiceImpl;

import jakarta.validation.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceImplTest {
    @Mock AbsenceRequestRepository requests;
    @Mock AbsenceApproverAssignmentRepository assignments;
    @Mock UserRepository users;
    @Mock AbsenceMailRepository mail;
    AbsenceServiceImpl service;
    ValidatorFactory factory;
    User employee, approver;

    @BeforeEach
    void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        service =
                new AbsenceServiceImpl(requests, assignments, users, mail, factory.getValidator());
        employee = user("employee");
        approver = user("approver");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        factory.close();
    }

    private User user(String id) {
        return User.builder()
                .id(id)
                .username(id.toUpperCase())
                .firstName(id)
                .lastName("Demo")
                .status("ACTIVE")
                .email(id + "@example.test")
                .build();
    }

    private void login(User u) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(u.getUsername(), null, List.of()));
        when(users.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
    }

    private AbsenceCreateRequest input() {
        var r = new AbsenceCreateRequest();
        r.setType("PERSONAL");
        r.setReason("Personal appointment");
        r.setStartDate(LocalDate.of(2026, 10, 1));
        r.setEndDate(LocalDate.of(2026, 10, 1));
        return r;
    }

    private AbsenceRequest pending() {
        var r = new AbsenceRequest();
        r.setId("request");
        r.setRequester(employee);
        r.setApprover(approver);
        r.setStatus(AbsenceStatus.PENDING);
        return r;
    }

    private void assign() {
        var a = new AbsenceApproverAssignment();
        a.setEmployee(employee);
        a.setApprover(approver);
        when(assignments.findByEmployee_IdAndActiveTrue(employee.getId()))
                .thenReturn(Optional.of(a));
    }

    private void save() {
        when(requests.saveAndFlush(any()))
                .thenAnswer(
                        i -> {
                            AbsenceRequest r = i.getArgument(0);
                            if (r.getId() == null) r.setId("new-request");
                            return r;
                        });
    }

    @Test
    void createsForAuthenticatedEmployeeAndQueuesApproverEmail() {
        login(employee);
        assign();
        save();
        var response = service.create(input());
        assertThat(response.getRequesterId()).isEqualTo("employee");
        assertThat(response.getApproverId()).isEqualTo("approver");
        assertThat(response.getStatus()).isEqualTo(AbsenceStatus.PENDING);
        assertThat(response.getSupportFile()).isNull();
        var captor = ArgumentCaptor.forClass(AbsenceMail.class);
        verify(mail).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo("approver@example.test");
        assertThat(captor.getValue().getBody())
                .contains("new-request")
                .doesNotContain("Personal appointment");
    }

    @Test
    void rejectsMissingAssignment() {
        login(employee);
        when(assignments.findByEmployee_IdAndActiveTrue("employee")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(input())).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(requests, mail);
    }

    @Test
    void rejectsInactiveApprover() {
        login(employee);
        assign();
        approver.setStatus("INACTIVE");
        assertThatThrownBy(() -> service.create(input())).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(requests, mail);
    }

    @Test
    void rejectsSelfApprovalAssignment() {
        login(employee);
        approver = employee;
        assign();
        assertThatThrownBy(() -> service.create(input())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsAnonymousAccess() {
        assertThatThrownBy(() -> service.findMyRequests())
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(users, requests);
    }

    @Test
    void rejectsInactiveAuthenticatedUser() {
        login(employee);
        employee.setStatus("INACTIVE");
        assertThatThrownBy(() -> service.findMyRequests())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsInvertedDates() {
        login(employee);
        var r = input();
        r.setEndDate(r.getStartDate().minusDays(1));
        assertThatThrownBy(() -> service.create(r)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(requests);
    }

    @Test
    void rejectsPartialTimes() {
        login(employee);
        var r = input();
        r.setStartTime(LocalTime.NOON);
        assertThatThrownBy(() -> service.create(r)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsEqualTimes() {
        login(employee);
        var r = input();
        r.setStartTime(LocalTime.NOON);
        r.setEndTime(LocalTime.NOON);
        assertThatThrownBy(() -> service.create(r)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsSeconds() {
        login(employee);
        var r = input();
        r.setStartTime(LocalTime.of(9, 0, 1));
        r.setEndTime(LocalTime.NOON);
        assertThatThrownBy(() -> service.create(r)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void acceptsMinutePrecisionAndOvernightRanges() {
        login(employee);
        assign();
        save();
        var r = input();
        r.setStartTime(LocalTime.of(23, 45));
        r.setEndDate(r.getStartDate().plusDays(1));
        r.setEndTime(LocalTime.of(0, 15));
        assertThat(service.create(r).getEndTime()).isEqualTo(LocalTime.of(0, 15));
    }

    @Test
    void listsOnlyAuthenticatedEmployeeRequests() {
        login(employee);
        when(requests.findByRequester_IdOrderByCreatedAtDesc("employee"))
                .thenReturn(List.of(pending()));
        assertThat(service.findMyRequests()).hasSize(1);
    }

    @Test
    void listsOnlyAssignedPendingRequests() {
        login(approver);
        when(requests.findByApprover_IdAndStatusOrderByCreatedAtAsc(
                        "approver", AbsenceStatus.PENDING))
                .thenReturn(List.of(pending()));
        assertThat(service.findPendingApprovals()).hasSize(1);
    }

    @Test
    void approvesAndQueuesEmployeeEmail() {
        login(approver);
        var r = pending();
        when(requests.findForDecision("request")).thenReturn(Optional.of(r));
        save();
        var d = new AbsenceDecisionRequest();
        d.setComment(" Approved ");
        var result = service.approve("request", d);
        assertThat(result.getStatus()).isEqualTo(AbsenceStatus.APPROVED);
        assertThat(result.getApprovedAt()).isNotNull();
        assertThat(result.getApprovalComment()).isEqualTo("Approved");
        verify(mail)
                .save(
                        argThat(
                                m ->
                                        m.getRecipient().equals("employee@example.test")
                                                && m.getBody().contains("aprobada")));
    }

    @Test
    void rejectsAndQueuesEmployeeEmailWithoutApprovedTimestamp() {
        login(approver);
        when(requests.findForDecision("request")).thenReturn(Optional.of(pending()));
        save();
        var result = service.reject("request", new AbsenceDecisionRequest());
        assertThat(result.getStatus()).isEqualTo(AbsenceStatus.REJECTED);
        assertThat(result.getApprovedAt()).isNull();
        verify(mail).save(argThat(m -> m.getBody().contains("rechazada")));
    }

    @Test
    void deniesUnassignedDecision() {
        login(employee);
        when(requests.findForDecision("request")).thenReturn(Optional.of(pending()));
        assertThatThrownBy(() -> service.approve("request", new AbsenceDecisionRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verify(requests, never()).saveAndFlush(any());
        verifyNoInteractions(mail);
    }

    @ParameterizedTest
    @EnumSource(
            value = AbsenceStatus.class,
            names = {"APPROVED", "REJECTED", "CANCELLED"})
    void deniesAllTerminalTransitions(AbsenceStatus state) {
        login(approver);
        var r = pending();
        r.setStatus(state);
        when(requests.findForDecision("request")).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.approve("request", new AbsenceDecisionRequest()))
                .isInstanceOf(AbsenceConflictException.class);
        assertThatThrownBy(() -> service.reject("request", new AbsenceDecisionRequest()))
                .isInstanceOf(AbsenceConflictException.class);
        verifyNoInteractions(mail);
    }

    @Test
    void reportsMissingRequest() {
        login(approver);
        when(requests.findForDecision("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reject("missing", new AbsenceDecisionRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsOversizedComment() {
        login(approver);
        var r = new AbsenceDecisionRequest();
        r.setComment("x".repeat(2001));
        assertThatThrownBy(() -> service.approve("request", r))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(requests);
    }
}
