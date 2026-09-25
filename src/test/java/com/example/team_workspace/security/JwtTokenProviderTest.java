package com.example.team_workspace.security;

import java.time.Duration;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-signing-key-with-more-than-32-bytes";

    @Test
    void rejectsTokenWithoutExpiration() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, Duration.ofHours(1)));
        UserDetails userDetails = User.withUsername("user@example.com")
                .password("encoded-password")
                .roles("USER")
                .build();
        String tokenWithoutExpiration = Jwts.builder()
                .subject(userDetails.getUsername())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(provider.isTokenValid(tokenWithoutExpiration, userDetails)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, Duration.ofMinutes(1)));
        UserDetails userDetails = User.withUsername("user@example.com")
                .password("encoded-password")
                .roles("USER")
                .build();
        String expiredToken = Jwts.builder()
                .subject(userDetails.getUsername())
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(provider.isTokenValid(expiredToken, userDetails)).isFalse();
    }
}
