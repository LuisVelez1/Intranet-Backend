package com.backendintranet.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.backendintranet.dto.request.AbsenceCreateRequest;
import com.backendintranet.dto.request.AbsenceDecisionRequest;
import com.backendintranet.entity.*;
import com.backendintranet.repository.*;
import com.backendintranet.service.impl.AbsenceMailDispatcher;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest(
        properties = {
            "absence.mail.enabled=true", "absence.mail.host=localhost",
            "absence.mail.from=intranet@example.test", "absence.mail.poll-ms=86400000",
            "spring.datasource.url=jdbc:h2:mem:absence_transaction_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
        })
class AbsenceTransactionIntegrationTest {
    @Autowired AbsenceService service;
    @Autowired AbsenceMailDispatcher dispatcher;
    @Autowired UserRepository users;
    @Autowired AbsenceRequestRepository requests;
    @Autowired AbsenceApproverAssignmentRepository assignments;
    @Autowired AbsenceMailRepository mail;
    @Autowired AbsenceTypeRepository absenceTypes;
    @Autowired PlatformTransactionManager transactionManager;

    @MockitoBean(name = "absenceMailSender")
    JavaMailSenderImpl sender;

    TransactionTemplate transaction;
    User employee, approver;

    @BeforeEach
    void setup() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(
                status -> {
                    employee = saveUser("TRANSACTION_EMPLOYEE");
                    approver = saveUser("TRANSACTION_APPROVER");
                    var assignment = new AbsenceApproverAssignment();
                    assignment.setEmployee(employee);
                    assignment.setApprover(approver);
                    assignments.saveAndFlush(assignment);

                    var personalType = new AbsenceType();
                    personalType.setName("PERSONAL");
                    personalType.setDescription("Permiso personal de prueba");
                    personalType.setActive(true);
                    absenceTypes.saveAndFlush(personalType);
                });
        login(employee);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        transaction.executeWithoutResult(
                status -> {
                    mail.deleteAll();
                    requests.deleteAll();
                    assignments.deleteAll();
                    absenceTypes.deleteAll();
                    users.delete(employee);
                    users.delete(approver);
                });
    }

    private User saveUser(String username) {
        return users.saveAndFlush(
                User.builder()
                        .username(username)
                        .firstName("Demo")
                        .lastName(username)
                        .email(username + "@example.test")
                        .password("unused")
                        .status("ACTIVE")
                        .roles(new HashSet<>())
                        .build());
    }

    private void login(User user) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(), null, List.of()));
    }

private AbsenceCreateRequest input() {
    var input = new AbsenceCreateRequest();
    input.setType("PERSONAL");
    input.setReason("Appointment");
    input.setStartDate(LocalDate.of(2026, 10, 1));
    input.setEndDate(input.getStartDate());
    input.setStartTime(LocalTime.of(9, 0));
    input.setEndTime(LocalTime.of(10, 0));
    return input;
}
    @Test
    void rollbackRemovesBothRequestAndNotification() {
        transaction.executeWithoutResult(
                status -> {
                    service.create(input());
                    assertThat(requests.count()).isEqualTo(1);
                    assertThat(mail.count()).isEqualTo(1);
                    status.setRollbackOnly();
                });
        assertThat(requests.count()).isZero();
        assertThat(mail.count()).isZero();
        dispatcher.dispatch();
        verifyNoInteractions(sender);
    }

    @Test
    void committedNotificationIsSentOnceAcrossPollingCycles() {
        service.create(input());
        verifyNoInteractions(sender);
        dispatcher.dispatch();
        dispatcher.dispatch();
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
        assertThat(mail.findAll().getFirst().getSentAt()).isNotNull();
    }

    @Test
    void concurrentDecisionsProduceOneWinnerAndOneConflict() throws Exception {
        String id = service.create(input()).getId();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> decision =
                    () -> {
                        login(approver);
                        ready.countDown();
                        try {
                            if (!start.await(10, TimeUnit.SECONDS))
                                throw new IllegalStateException("Start timed out");
                            service.approve(id, new AbsenceDecisionRequest());
                            return "approved";
                        } catch (com.backendintranet.exception.AbsenceConflictException e) {
                            return "conflict";
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    };
            Future<String> first = executor.submit(decision);
            Future<String> second = executor.submit(decision);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("approved", "conflict");
        }
        /*
         * La primera notificación fue creada al registrar
         * la solicitud para el jefe inmediato.
         *
         * En este test no hay destinatario HR configurado,
         * por lo que la aprobación del jefe no agrega
         * otro correo al outbox.
         */
        assertThat(mail.count()).isEqualTo(1);

        var persisted =
                requests.findById(id).orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(AbsenceStatus.PENDING_HR);

        assertThat(persisted.getBossDecision())
                .isEqualTo("APPROVED");

        assertThat(persisted.getBossDecisionAt())
                .isNotNull();

        assertThat(persisted.getApprovedAt())
                .isNull();
    }
}
