package com.lifetracker.infrastructure.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the RS256 public key as a JWKS, so a resource server (this app now, an external one or
 * a rotated key later) can verify access tokens without holding a shared secret. Public key only —
 * {@code toPublicJWK()} strips the private material.
 */
@RestController
class JwksController {

    private final RSAKey rsaJwk;

    JwksController(RSAKey rsaJwk) {
        this.rsaJwk = rsaJwk;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return new JWKSet(rsaJwk.toPublicJWK()).toJSONObject();
    }
}
