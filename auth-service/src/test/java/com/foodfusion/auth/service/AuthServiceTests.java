package com.foodfusion.auth.service;

import com.foodfusion.auth.dto.AuthResponse;
import com.foodfusion.auth.dto.LoginRequest;
import com.foodfusion.auth.dto.RegisterRequest;
import com.foodfusion.auth.model.UserAccount;
import com.foodfusion.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, jwtEncoder, "http://localhost:8085");
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-signed-token")
                .header("alg", "HS256")
                .subject("user-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
    }

    @Test
    void registerNormalizesEmailAndStoresOnlyEncodedPassword() {
        when(users.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("long-enough-password")).thenReturn("bcrypt-hash");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            saved.setId("user-123");
            return saved;
        });

        AuthResponse response = authService.register(new RegisterRequest(
                " Customer@Example.com ", "  Casey  ", "long-enough-password"));

        assertEquals("test-signed-token", response.accessToken());
        assertEquals("customer@example.com", response.email());
        assertEquals("Casey", response.displayName());
        assertEquals("user-123", response.userId());
        verify(passwordEncoder).encode("long-enough-password");
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserAccount user = new UserAccount();
        user.setEmail("customer@example.com");
        user.setPasswordHash("bcrypt-hash");
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("Customer@Example.com", "wrong-password")));

        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void loginIssuesJwtWithVerifiableSignatureAndClaims() throws Exception {
        String configuredSecret = "unit-test-jwt-secret-that-is-long-enough";
        SecretKey key = new SecretKeySpec(
                MessageDigest.getInstance("SHA-256")
                        .digest(configuredSecret.getBytes(StandardCharsets.UTF_8)),
                "HmacSHA256");
        JwtEncoder realEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtDecoder realDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        UserAccount user = new UserAccount();
        user.setId("user-123");
        user.setEmail("customer@example.com");
        user.setDisplayName("Casey");
        user.setPasswordHash("bcrypt-hash");
        user.setRoles(java.util.List.of("USER"));
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("long-enough-password", "bcrypt-hash")).thenReturn(true);
        AuthService serviceWithSigningKey = new AuthService(
                users, passwordEncoder, realEncoder, "http://localhost:8085");

        AuthResponse response = serviceWithSigningKey.login(
                new LoginRequest("customer@example.com", "long-enough-password"));
        Jwt decoded = realDecoder.decode(response.accessToken());

        assertEquals("http://localhost:8085", decoded.getIssuer().toString());
        assertEquals("user-123", decoded.getSubject());
        assertEquals("customer@example.com", decoded.getClaimAsString("email"));
    }
}
