package com.backendintranet.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.backendintranet.dto.request.AbsenceCreateRequest;
import com.backendintranet.dto.response.AbsenceResponse;
import com.backendintranet.entity.AbsenceStatus;
import com.backendintranet.exception.AbsenceConflictException;
import com.backendintranet.service.AbsenceService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AbsenceControllerContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean AbsenceService service;

    @Test
    void createsAtApiContextAndReturnsCreatedResponse() throws Exception {
        when(service.create(any()))
                .thenReturn(
                        AbsenceResponse.builder().id("uuid").status(AbsenceStatus.PENDING).build());
        mvc.perform(
                        post("/api/absences")
                                .contextPath("/api")
                                .with(user("EMPLOYEE"))
                                .contentType("application/json")
                                .content(
                                        "{\"type\":\"PERSONAL\",\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-01\",\"startTime\":\"09:15\",\"endTime\":\"10:30\",\"reason\":\"Appointment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
        var captor = ArgumentCaptor.forClass(AbsenceCreateRequest.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getStartTime().getMinute()).isEqualTo(15);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"type\":\"PERSONAL\",\"startDate\":\"2026-10-02\",\"endDate\":\"2026-10-01\",\"reason\":\"Appointment\"}",
                "{\"type\":\"PERSONAL\",\"startDate\":\"2026-02-30\",\"endDate\":\"2026-03-01\",\"reason\":\"Appointment\"}",
                "{\"type\":\"PERSONAL\",\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-01\",\"startTime\":\"09:15\",\"reason\":\"Appointment\"}"
            })
    void rejectsInvalidBodies(String body) throws Exception {
        mvc.perform(
                        post("/absences")
                                .with(user("EMPLOYEE"))
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsOversizedDecisionComment() throws Exception {
        mvc.perform(
                        put("/absences/id/reject")
                                .with(user("APPROVER"))
                                .contentType("application/json")
                                .content("{\"comment\":\"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsEmptyOptionalComment() throws Exception {
        mvc.perform(
                        put("/absences/id/approve")
                                .with(user("APPROVER"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isOk());
        verify(service).approve(eq("id"), any());
    }

    @Test
    void returnsConflictForRepeatedDecision() throws Exception {
        when(service.reject(eq("id"), any()))
                .thenThrow(new AbsenceConflictException("Already decided"));
        mvc.perform(
                        put("/absences/id/reject")
                                .with(user("APPROVER"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void exposesBothScopedLists() throws Exception {
        mvc.perform(get("/absences/my").with(user("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/absences/pending").with(user("APPROVER"))).andExpect(status().isOk());
        verify(service).findMyRequests();
        verify(service).findPendingApprovals();
    }
}
