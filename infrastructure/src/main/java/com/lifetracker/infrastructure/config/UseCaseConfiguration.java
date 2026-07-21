package com.lifetracker.infrastructure.config;

import com.lifetracker.application.user.Authenticate;
import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the plain-Java use cases as Spring beans. The use cases carry no Spring annotations — the
 * application module has no Spring on its classpath — so infrastructure constructs them here from
 * the port beans ({@code JpaUserRepository}, {@code Argon2idPasswordHasher}).
 */
@Configuration
class UseCaseConfiguration {

    @Bean
    RegisterUser registerUser(UserRepository users, PasswordHasher passwordHasher) {
        return new RegisterUser(users, passwordHasher);
    }

    @Bean
    Authenticate authenticate(UserRepository users, PasswordHasher passwordHasher) {
        return new Authenticate(users, passwordHasher);
    }
}
