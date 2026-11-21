package com.showroom.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring context or database needed. */
class JwtUtilTest {

    private static final String SECRET =
            "wtBKz1tndgvK2TsogXRiyUs2sfVxOjaOETxpI0l5tQoURqV1wt6knDIyTTDH/cCQkZJWCxhESv6SPkFWxTvTNQ==";

    @Test
    void generatesAndValidatesToken() {
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        UserDetails user = User.withUsername("admin@showroom.com").password("x").authorities(List.of()).build();

        String token = jwt.generateToken(user.getUsername(), Map.of("role", "ADMIN"));

        assertThat(jwt.extractUsername(token)).isEqualTo("admin@showroom.com");
        assertThat(jwt.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsTokenForDifferentUser() {
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        UserDetails other = User.withUsername("someone@else.com").password("x").authorities(List.of()).build();

        String token = jwt.generateToken("admin@showroom.com", Map.of());

        assertThat(jwt.isTokenValid(token, other)).isFalse();
    }
}
