package com.backendintranet.config.security;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.backendintranet.dto.request.*;
import com.backendintranet.entity.*;
import com.backendintranet.repository.*;
import com.backendintranet.service.AbsenceService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AbsenceSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AbsenceRequestRepository requests;
    @Autowired AbsenceApproverAssignmentRepository assignments;
    @Autowired AbsenceMailRepository mail;
    @Autowired AbsenceTypeRepository absenceTypes;
    @Autowired RoleRepository roles;
    @Autowired AbsenceService service;
    User employee, approver, outsider;

    @BeforeEach
    void setup() {
        employee = saveUser("ABSENCE_EMPLOYEE");
        approver = saveUser("ABSENCE_APPROVER");
        outsider = saveUser("ABSENCE_OUTSIDER");
        var assignment = new AbsenceApproverAssignment();
        assignment.setEmployee(employee);
        assignment.setApprover(approver);
        assignments.saveAndFlush(assignment);

        var personalType = new AbsenceType();
        personalType.setName("PERSONAL");
        personalType.setDescription("Permiso personal de prueba");
        personalType.setActive(true);
        absenceTypes.saveAndFlush(personalType);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private User saveUser(String name) {
        return users.saveAndFlush(
                User.builder()
                        .username(name)
                        .firstName(name)
                        .lastName("Test")
                        .email(name + "@example.test")
                        .password("unused-test-password")
                        .status("ACTIVE")
                        .roles(new HashSet<>())
                        .build());
    }

private String createRequest() {
    SecurityContextHolder.getContext()
            .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            employee.getUsername(), null, List.of()));

    var input = new AbsenceCreateRequest();
    input.setType("PERSONAL");
    input.setReason("Appointment");
    input.setStartDate(LocalDate.of(2026, 10, 1));
    input.setEndDate(input.getStartDate());
    input.setStartTime(LocalTime.of(9, 0));
    input.setEndTime(LocalTime.of(10, 0));

    return service.create(input).getId();
}
    private void grantRole(
            User user,
            String roleName) {

        Role role =
                roles.findByName(roleName)
                        .orElseGet(
                                () ->
                                        roles.saveAndFlush(
                                                Role.builder()
                                                        .name(roleName)
                                                        .build()));

        if (user.getRoles() == null) {
            user.setRoles(
                    new HashSet<>());
        }

        user.getRoles()
                .add(role);

        users.saveAndFlush(
                user);
    }

    private MockMultipartFile pngSupport() {

        byte[] png =
                new byte[] {
                    (byte) 0x89,
                    0x50,
                    0x4E,
                    0x47,
                    0x0D,
                    0x0A,
                    0x1A,
                    0x0A,
                    0x00
                };

        return new MockMultipartFile(
                "file",
                "soporte.png",
                "image/png",
                png);
    }

    private String createRequestWithSupport() {

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                employee.getUsername(),
                                null,
                                List.of()));

        var input =
                new AbsenceCreateRequest();

        input.setType("PERSONAL");
        input.setReason("Appointment");
        input.setStartDate(
                LocalDate.of(
                        2026,
                        10,
                        1));
        input.setEndDate(
                input.getStartDate());
        input.setStartTime(
                LocalTime.of(
                        9,
                        0));
        input.setEndTime(
                LocalTime.of(
                        10,
                        0));

        return service
                .create(
                        input,
                        pngSupport())
                .getId();
    }

    @Test
    void blocksAllEndpointsForAnonymousUsers() throws Exception {
        mvc.perform(get("/absences/my")).andExpect(status().isForbidden());
        mvc.perform(get("/absences/pending")).andExpect(status().isForbidden());
        mvc.perform(post("/absences").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/absences/id/approve").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/absences/id/reject").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

@Test
void createsPersistsAndPreventsRequesterSpoofing() throws Exception {
    mvc.perform(
                    post("/api/absences")
                            .contextPath("/api")
                            .with(user(employee.getUsername()))
                            .contentType("application/json")
                            .content(
                                    "{\"type\":\"PERSONAL\","
                                            + "\"startDate\":\"2026-10-01\","
                                            + "\"endDate\":\"2026-10-01\","
                                            + "\"startTime\":\"09:00\","
                                            + "\"endTime\":\"10:00\","
                                            + "\"reason\":\"Appointment\","
                                            + "\"requesterId\":\"forged\","
                                            + "\"approverId\":\"forged\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requesterId").value(employee.getId()))
            .andExpect(jsonPath("$.approverId").value(approver.getId()));

    assertThat(
                    requests.findByRequester_IdOrderByCreatedAtDesc(
                            employee.getId()))
            .hasSize(1);

    assertThat(mail.findAll()).hasSize(1);
    assertThat(mail.findAll().getFirst().getSentAt()).isNull();
}
    @Test
    void isolatesPersonalAndPendingLists() throws Exception {
        String id = createRequest();
        mvc.perform(get("/absences/my").with(user(outsider.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/absences/pending").with(user(employee.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/absences/pending").with(user(approver.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void refusesUnassignedUserEvenWithSuperAdminRole() throws Exception {
        String id = createRequest();
        mvc.perform(
                        put("/absences/" + id + "/approve")
                                .with(user(outsider.getUsername()).roles("SUPER_ADMIN"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isForbidden());
        assertThat(requests.findById(id).orElseThrow().getStatus())
                .isEqualTo(AbsenceStatus.PENDING);
        assertThat(mail.count()).isEqualTo(1);
    }

    @Test
    void assignedApproverCanApproveOnlyOnceAndMovesRequestToHr() throws Exception {

        String id = createRequest();

        mvc.perform(
                        put("/absences/" + id + "/approve")
                                .with(user(approver.getUsername()))
                                .contentType("application/json")
                                .content("{\"comment\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING_HR"))
                .andExpect(
                        jsonPath("$.bossDecision")
                                .value("APPROVED"))
                .andExpect(
                        jsonPath("$.bossDecisionAt")
                                .isNotEmpty())
                .andExpect(
                        jsonPath("$.bossComment")
                                .value("OK"))
                .andExpect(
                        jsonPath("$.approvedAt")
                                .doesNotExist());

        /*
         * El jefe ya tomó su decisión.
         * No puede decidir nuevamente.
         */
        mvc.perform(
                        put("/absences/" + id + "/reject")
                                .with(user(approver.getUsername()))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isConflict());

        var persisted =
                requests.findById(id).orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(
                        AbsenceStatus.PENDING_HR);

        assertThat(persisted.getBossDecision())
                .isEqualTo("APPROVED");

        assertThat(persisted.getApprovedAt())
                .isNull();

        /*
         * En esta prueba no está configurado el correo
         * de Talento Humano, por lo que permanece solo
         * la notificación inicial al jefe.
         */
        assertThat(mail.count())
                .isEqualTo(1);
    }

    @Test
    void assignedApproverCanRejectAndRequestDisappearsFromPending() throws Exception {
        String id = createRequest();
        mvc.perform(
                        put("/absences/" + id + "/reject")
                                .with(user(approver.getUsername()))
                                .contentType("application/json")
                                .content("{\"comment\":\"Reschedule\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(
                        requests.findByApprover_IdAndStatusOrderByCreatedAtAsc(
                                approver.getId(), AbsenceStatus.PENDING))
                .isEmpty();
    }

@Test
void inactiveAssignmentPreventsCreation() throws Exception {
    var assignment =
            assignments.findByEmployee_IdAndActiveTrue(employee.getId())
                    .orElseThrow();

    assignment.setActive(false);
    assignments.saveAndFlush(assignment);

    mvc.perform(
                    post("/absences")
                            .with(user(employee.getUsername()))
                            .contentType("application/json")
                            .content(
                                    "{\"type\":\"PERSONAL\","
                                            + "\"startDate\":\"2026-10-01\","
                                            + "\"endDate\":\"2026-10-01\","
                                            + "\"startTime\":\"09:00\","
                                            + "\"endTime\":\"10:00\","
                                            + "\"reason\":\"Appointment\"}"))
            .andExpect(status().isBadRequest());

    assertThat(mail.count()).isZero();
}
    @Test
    void inactiveUserCannotUseAnExistingAuthenticatedContext() throws Exception {
        employee.setStatus("INACTIVE");
        users.saveAndFlush(employee);
        mvc.perform(get("/absences/my").with(user(employee.getUsername())))
                .andExpect(status().isForbidden());
    }
    @Test
    void createsMultipartRequestWithPngSupport()
            throws Exception {

        var data =
                new MockMultipartFile(
                        "data",
                        "",
                        MediaType.APPLICATION_JSON_VALUE,
                        (
                                "{"
                                        + "\"type\":\"PERSONAL\","
                                        + "\"startDate\":\"2026-10-01\","
                                        + "\"endDate\":\"2026-10-01\","
                                        + "\"startTime\":\"09:00\","
                                        + "\"endTime\":\"10:00\","
                                        + "\"reason\":\"Appointment\""
                                        + "}")
                                .getBytes());

        mvc.perform(
                        multipart("/absences")
                                .file(data)
                                .file(
                                        pngSupport())
                                .with(
                                        user(
                                                employee.getUsername())))
                .andExpect(
                        status().isCreated())
                .andExpect(
                        jsonPath("$.supportFile")
                                .isNotEmpty());

        var stored =
                requests
                        .findByRequester_IdOrderByCreatedAtDesc(
                                employee.getId())
                        .getFirst();

        assertThat(
                        stored.getSupportFile())
                .isNotBlank()
                .endsWith(".png");
    }

    @Test
    void requesterCanDownloadOwnSupport()
            throws Exception {

        String id =
                createRequestWithSupport();

        mvc.perform(
                        get(
                                "/absences/"
                                        + id
                                        + "/support")
                                .with(
                                        user(
                                                employee.getUsername())))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().contentType(
                                MediaType.IMAGE_PNG))
                .andExpect(
                        header()
                                .string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.containsString(
                                                "attachment")));
    }

    @Test
    void assignedApproverCanDownloadSupport()
            throws Exception {

        String id =
                createRequestWithSupport();

        mvc.perform(
                        get(
                                "/absences/"
                                        + id
                                        + "/support")
                                .with(
                                        user(
                                                approver.getUsername())))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().contentType(
                                MediaType.IMAGE_PNG));
    }

    @Test
    void unrelatedUserCannotDownloadSupport()
            throws Exception {

        String id =
                createRequestWithSupport();

        mvc.perform(
                        get(
                                "/absences/"
                                        + id
                                        + "/support")
                                .with(
                                        user(
                                                outsider.getUsername())))
                .andExpect(
                        status().isForbidden());
    }

    @Test
    void requestWithoutSupportReturnsNotFound()
            throws Exception {

        String id =
                createRequest();

        mvc.perform(
                        get(
                                "/absences/"
                                        + id
                                        + "/support")
                                .with(
                                        user(
                                                employee.getUsername())))
                .andExpect(
                        status().isNotFound());
    }


    @Test
    void talentHumanCanDownloadAnyAbsenceSupport()
            throws Exception {

        String id =
                createRequestWithSupport();

        grantRole(
                outsider,
                "TALENTO_HUMANO");

        mvc.perform(
                        get(
                                "/absences/"
                                        + id
                                        + "/support")
                                .with(
                                        user(
                                                outsider.getUsername())))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().contentType(
                                MediaType.IMAGE_PNG));
    }

    @Test
    void superAdminCanDownloadAnyAbsenceSupport()
            throws Exception {

        String id =
                createRequestWithSupport();

        grantRole(
                outsider,
                "SUPER_ADMIN");

        mvc.perform(
                        get(
                                "/absences/"
                                        + id
                                        + "/support")
                                .with(
                                        user(
                                                outsider.getUsername())))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().contentType(
                                MediaType.IMAGE_PNG));
    }


}
