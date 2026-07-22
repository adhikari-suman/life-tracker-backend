package com.lifetracker.infrastructure.persistence.user;

import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Read side for Users — returns a flat {@link UserView}, never the domain aggregate. Reads live in
 * query services, not use cases (which are for writes).
 */
@Component
public class UserQueryService {

    private final UserJpaData data;

    UserQueryService(UserJpaData data) {
        this.data = data;
    }

    public Optional<UserView> findById(UserId id) {
        return data.findById(id.value())
                .map(e -> new UserView(e.getId(), e.getEmail(), e.isEmailVerified(), e.getCreatedAt()));
    }
}
