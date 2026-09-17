package com.backendintranet.config.security;

import com.backendintranet.service.ReservationService;
import com.backendintranet.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean ReservationService reservationService;

    @Test
    void authenticationEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealthIsAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedDirectoryRejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/users/directory"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedDirectoryAllowsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/users/directory").with(user("JDOE").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void unauthorizedReservationCancellationReturnsForbiddenErrorResponse() throws Exception {
        when(reservationService.cancel("r1", "OTHER"))
                .thenThrow(new AccessDeniedException("No tienes permiso para cancelar esta reserva"));

        mockMvc.perform(patch("/reservations/r1/cancel").with(user("OTHER").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void regularUserCannotAccessAdminRegistration() throws Exception {
        mockMvc.perform(post("/users/register").with(user("JDOE").roles("USER"))
                        .with(csrf()).contentType("application/json").content("{}"))
                 .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessRegistrationEndpoint() throws Exception {
        mockMvc.perform(post("/users/register").with(user("ADMIN").roles("ADMIN"))
                        .with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminCanAccessRegistrationEndpoint() throws Exception {
        mockMvc.perform(post("/users/register").with(user("ROOT").roles("SUPER_ADMIN"))
                        .with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }
}