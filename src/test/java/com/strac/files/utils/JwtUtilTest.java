package com.strac.files.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Charles on 27/12/2024
 */

@SpringBootTest
public class JwtUtilTest {

    private JwtUtil jwtUtil;

    private final String secretKey = "testsecretkeyfortokenvalidation12345";
    private String userId;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        // Inject private fields using reflection
        Field secretKeyField = JwtUtil.class.getDeclaredField("SECRET_KEY");
        secretKeyField.setAccessible(true);
        secretKeyField.set(jwtUtil, secretKey);

        Field tokenExpiryField = JwtUtil.class.getDeclaredField("tokenExpirySec");
        tokenExpiryField.setAccessible(true);
        // 1 hour
        long tokenExpirySec = 3600L;
        tokenExpiryField.set(jwtUtil, tokenExpirySec);

        userId = "testUser";

        token = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (1000 * tokenExpirySec)))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void testExtractIdFromToken() {
        String extractedId = jwtUtil.extractIdFromToken(token);
        assertEquals(userId, extractedId, "Extracted user ID should match the provided ID.");
    }

    @Test
    void testExtractExpiration() {
        Date expiration = jwtUtil.extractExpiration(token);
        assertNotNull(expiration, "Expiration date should not be null.");
        assertTrue(expiration.after(new Date()), "Expiration date should be in the future.");
    }

    @Test
    void testIsTokenExpired_NotExpired() {
        boolean isExpired = jwtUtil.isTokenExpired(token);
        assertFalse(isExpired, "Token should not be expired.");
    }

    @Test
    void testIsTokenExpired_Expired() throws InterruptedException {
        String expiredToken = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        boolean isExpired = jwtUtil.isTokenExpired(expiredToken);
        assertTrue(isExpired, "Token should be expired.");
    }

    @Test
    void testGenerateToken() {
        String generatedToken = jwtUtil.generateToken(userId);
        assertNotNull(generatedToken, "Generated token should not be null.");
        String extractedId = jwtUtil.extractIdFromToken(generatedToken);
        assertEquals(userId, extractedId, "Extracted user ID from generated token should match the provided ID.");
    }

    @Test
    void testValidateToken_ValidToken() {
        boolean isValid = jwtUtil.validateToken(token, userId);
        assertTrue(isValid, "Token should be valid for the provided user ID.");
    }

    @Test
    void testValidateToken_InvalidUserId() {
        boolean isValid = jwtUtil.validateToken(token, "anotherUser");
        assertFalse(isValid, "Token should not be valid for a different user ID.");
    }

    @Test
    void testValidateToken_ExpiredToken() {
        String expiredToken = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        boolean isValid = jwtUtil.validateToken(expiredToken, userId);
        assertFalse(isValid, "Token should not be valid as it is expired.");
    }
}
