package com.blastradius.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-testing-purposes-only";
    private static final long EXPIRATION = 3600000L;        // 1 hour
    private static final long REFRESH_EXP = 7200000L;       // 2 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION, REFRESH_EXP);
    }

    @Test
    void generateAndValidateToken_shouldSucceed() {
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String username = "john.doe";
        String token = jwtTokenProvider.generateTokenFromUsername(username);

        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void validateToken_invalidToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt"));
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {
        JwtTokenProvider shortExpiry = new JwtTokenProvider(SECRET, 1L, REFRESH_EXP); // 1ms expiry
        String token = shortExpiry.generateTokenFromUsername("testuser");

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertFalse(shortExpiry.validateToken(token));
    }

    @Test
    void generateRefreshToken_shouldBeValid() {
        String refresh = jwtTokenProvider.generateRefreshToken("adminuser");

        assertNotNull(refresh);
        assertTrue(jwtTokenProvider.validateToken(refresh));
        assertEquals("adminuser", jwtTokenProvider.getUsernameFromToken(refresh));
    }

    @Test
    void validateToken_emptyString_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void validateToken_null_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
