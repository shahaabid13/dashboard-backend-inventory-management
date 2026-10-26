package com.inventory.msp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-signing-secret-long-enough-for-hs256";

    @Test
    void tokenContainsIdentityAndRolesAndNeverExpiresAfterThirtyMinutes() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiryMinutes", 45L);
        jwtUtil.validateExpiryConfiguration();

        String token = jwtUtil.generateToken("alice", "ADMIN");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("alice", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals(List.of("ADMIN"), claims.get("roles", List.class));
        assertTrue(claims.getExpiration().getTime() - claims.getIssuedAt().getTime() <= 30 * 60_000L);
        String secondToken = jwtUtil.generateToken("alice", "ADMIN");
        Claims secondClaims = Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes())
                .build()
                .parseClaimsJws(secondToken)
                .getBody();
        assertNotEquals(claims.getId(), secondClaims.getId());
    }

    @Test
    void expiryMustBePositive() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiryMinutes", 0L);

        assertThrows(IllegalArgumentException.class, jwtUtil::validateExpiryConfiguration);
    }
}
