package com.lifetracker.infrastructure.web.dto;

/**
 * Wire response for register / login / refresh. {@code refreshToken} is the {@code sessionId.secret}
 * form the client presents back to {@code /auth/refresh}. Matches the OpenAPI {@code TokenResponse}.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {

    public static TokenResponse bearer(String accessToken, long expiresIn, String refreshToken) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, refreshToken);
    }
}
