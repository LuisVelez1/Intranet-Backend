package com.backendintranet.util;

import com.backendintranet.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        String testKey = Base64.getEncoder().encodeToString("phase-2-test-signing-key-32-bytes!".getBytes());
        ReflectionTestUtils.setField(service, "secretKey", testKey);
        ReflectionTestUtils.setField(service, "jwtExpiration", 60_000L);
        user = User.builder().username("JDOE").build();
    }

    @Test
    void generatedTokenContainsUsernameSubject() {
        String token = service.generateToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("JDOE");
    }

    @Test
    void freshlyGeneratedTokenIsValidForMatchingUser() {
        String token = service.generateToken(user);

        assertThat(service.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentUsername() {
        String token = service.generateToken(user);
        User differentUser = User.builder().username("OTHER").build();

        assertThat(service.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    void extraClaimsCanBeGeneratedAndExtracted() {
        String token = service.generateToken(Map.of("department", "Operations"), user);

        String department = service.extractClaim(token, claims -> claims.get("department", String.class));
        assertThat(department).isEqualTo("Operations");
    }
}