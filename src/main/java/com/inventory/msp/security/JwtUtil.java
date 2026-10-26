package com.inventory.msp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final long MAX_EXPIRY_MINUTES = 30;
    private static final long MILLIS_PER_MINUTE = 60_000L;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiry-minutes:30}")
    private long jwtExpiryMinutes;

    @PostConstruct
    void validateExpiryConfiguration() {
        if (jwtExpiryMinutes <= 0) {
            throw new IllegalArgumentException("app.jwt.expiry-minutes must be greater than zero");
        }
    }

    private Key getSigningKey() {
        // ensure secret length is sufficient for HS256
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        long effectiveExpiryMinutes = Math.min(jwtExpiryMinutes, MAX_EXPIRY_MINUTES);
        Date expiry = new Date(now.getTime() + effectiveExpiryMinutes * MILLIS_PER_MINUTE);

        return Jwts.builder()
                .setSubject(username)
                .claim("username", username)
                .claim("roles", List.of(role))
                .claim("role", role)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // token invalid/expired/unsupported
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        return claims.get("role", String.class);
    }
}
