package com.lifetracker.infrastructure.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The security filter chain. A stateless bearer-token API: the public endpoints (register, login,
 * refresh, JWKS, email verification, password reset) are open; everything else needs a valid RS256
 * access token, validated by the
 * {@code JwtDecoder} bean. No cookies carry auth, so CSRF is off. The authenticated {@code Jwt}
 * carries the {@code sub} (the owner {@code UserId}) and {@code sid} (the current Session).
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    /**
     * The health endpoint, open. Actuator runs in the management context on its own port
     * ({@code management.server.port}), but a separate port is NOT a separate filter chain — Boot
     * propagates this configuration to the management context, so without this the chain below
     * answers 401 to the container's own healthcheck.
     * <p>
     * Scoped to the health endpoint by id rather than {@code toAnyEndpoint()}, so exposing a
     * second endpoint later does not silently make it public too. This cannot widen the API
     * surface: the management port serves no business endpoint, and 8080 maps nothing under
     * {@code /actuator}.
     */
    @Bean
    @Order(1)
    SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.to("health"))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh",
                                "/auth/verify-email", "/auth/password-reset", "/auth/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
