package com.lifetracker.infrastructure;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Base for full-context integration tests: real Postgres via {@link TestcontainersConfiguration},
 * and a fresh RS256 keypair generated per run and fed to {@code app.jwt.*} as temp PEM files. The
 * keypair is generated (never committed) so the tests exercise the real config key-loading path
 * (option B) without putting a private key in the repo — which the global {@code *.pem} ignore
 * would strip anyway.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, CapturingEmailSender.class})
abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        KeyPair keyPair = generator.generateKeyPair();

        // getEncoded() yields PKCS#8 (private) and X.509 (public) DER — exactly what JwtConfiguration parses.
        Path privatePem = writeTempPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        Path publicPem = writeTempPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        registry.add("app.jwt.private-key", () -> "file:" + privatePem);
        registry.add("app.jwt.public-key", () -> "file:" + publicPem);
        registry.add("app.jwt.issuer", () -> "life-tracker-test");
        registry.add("app.jwt.access-token-ttl", () -> "15m");
    }

    private static Path writeTempPem(String type, byte[] der) throws Exception {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        String pem = "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
        Path file = Files.createTempFile("jwt-", ".pem");
        file.toFile().deleteOnExit();
        Files.writeString(file, pem);
        return file;
    }
}
