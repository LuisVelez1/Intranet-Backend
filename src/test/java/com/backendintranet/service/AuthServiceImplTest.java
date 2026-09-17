package com.backendintranet.service;

import com.backendintranet.dto.request.LoginRequest;
import com.backendintranet.dto.response.AuthResponse;
import com.backendintranet.entity.Role;
import com.backendintranet.entity.User;
import com.backendintranet.exception.BadRequestException;
import com.backendintranet.repository.UserRepository;
import com.backendintranet.service.impl.AuthServiceImpl;
import com.backendintranet.util.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthServiceImpl service;

    @Test
    void loginNormalizesUsernameForAuthenticationAndLookup() {
        LoginRequest request = new LoginRequest();
        request.setUsuario("jdoe");
        request.setPassword("secret");
        User user = user("JDOE");
        when(userRepository.findByUsername("JDOE")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");

        AuthResponse response = service.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("JDOE");
        assertThat(captor.getValue().getCredentials()).isEqualTo("secret");
        verify(userRepository).findByUsername("JDOE");
        verify(jwtService).generateToken(user);
        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getUser()).isEqualTo("JDOE");
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void loginPreservesBadRequestWhenAuthenticatedUserIsMissing() {
        LoginRequest request = new LoginRequest();
        request.setUsuario("missing");
        request.setPassword("secret");
        when(userRepository.findByUsername("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadRequestException.class);
        verify(jwtService, never()).generateToken(any());
    }

    private static User user(String username) {
        Role userRole = Role.builder().name("USER").build();
        Role adminRole = Role.builder().name("ADMIN").build();
        return User.builder().id("user-1").username(username).firstName("Jane").lastName("Doe")
                .roles(Set.of(userRole, adminRole)).build();
    }
}