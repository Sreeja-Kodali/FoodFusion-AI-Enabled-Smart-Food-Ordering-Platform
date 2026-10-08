package com.foodfusion.auth.service;

import com.foodfusion.auth.dto.AuthResponse;
import com.foodfusion.auth.dto.LoginRequest;
import com.foodfusion.auth.dto.RegisterRequest;
import com.foodfusion.auth.model.UserAccount;
import com.foodfusion.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public AuthService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${spring.security.jwt.issuer}") String issuer
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(List.of("USER"));
        user.setCreatedAt(Instant.now());
        return issueToken(users.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        return issueToken(user);
    }

    public UserAccount createBootstrapAdmin(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        return users.findByEmail(normalizedEmail).orElseGet(() -> {
            UserAccount admin = new UserAccount();
            admin.setEmail(normalizedEmail);
            admin.setDisplayName("FoodFusion Administrator");
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRoles(List.of("ADMIN", "USER"));
            admin.setCreatedAt(Instant.now());
            return users.save(admin);
        });
    }

    private AuthResponse issueToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("roles", user.getRoles())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", TOKEN_TTL.toSeconds(), user.getId(),
                user.getEmail(), user.getDisplayName(), user.getRoles());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
