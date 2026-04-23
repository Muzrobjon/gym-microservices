package com.epam.gym.trainerworkloadservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private final String secret = "mySecretKeyForTestingPurposesOnlyAndItShouldBeLongEnough123456789";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", secret);
    }

    private String generateToken(String username, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = generateToken("john.doe", 60000);
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        String token = generateToken("john.doe", -1000);
        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertThat(jwtProvider.validateToken("")).isFalse();
    }

    @Test
    void getUsernameFromToken_returnsCorrectUsername() {
        String token = generateToken("john.doe", 60000);
        assertThat(jwtProvider.getUsernameFromToken(token)).isEqualTo("john.doe");
    }
}