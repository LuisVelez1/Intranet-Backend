package com.backendintranet.controller;

import com.backendintranet.service.RequirementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequirementControllerContractTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RequirementService requirementService;

    @Test
    void createRejectsMissingRequiredFieldsAtHttpBoundary() throws Exception {
        mockMvc.perform(post("/requirements").with(user("JDOE").roles("USER"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}