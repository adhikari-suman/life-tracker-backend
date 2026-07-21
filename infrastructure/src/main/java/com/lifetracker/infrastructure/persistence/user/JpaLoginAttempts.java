package com.lifetracker.infrastructure.persistence.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.LoginAttempts;
import com.lifetracker.domain.user.LoginThrottle;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The {@link LoginAttempts} port over the {@code login_attempts} table. Keys on the normalized email
 * string, so a miss is counted exactly like a wrong password and a lockout never reveals whether an
 * email is registered (ADR-0010).
 *
 * <p>Recording a failure first prunes that email's rows older than the throttle window: they can no
 * longer affect any decision, so this bounds the table to at most a window's worth per email. It
 * takes the window from the {@link LoginThrottle} policy so the retention horizon and the query
 * cutoff can never drift apart.
 */
@Repository
class JpaLoginAttempts implements LoginAttempts {

    private final LoginAttemptJpaData data;
    private final LoginThrottle throttle;

    JpaLoginAttempts(LoginAttemptJpaData data, LoginThrottle throttle) {
        this.data = data;
        this.throttle = throttle;
    }

    @Override
    public List<Instant> failuresSince(Email email, Instant cutoff) {
        return data.attemptedAtByEmailSince(email.value(), cutoff);
    }

    @Override
    @Transactional
    public void recordFailure(Email email, Instant at) {
        data.deleteByEmailAndAttemptedAtLessThan(email.value(), at.minus(throttle.window()));
        data.save(new LoginAttemptEntity(UUID.randomUUID(), email.value(), at));
    }

    @Override
    @Transactional
    public void clearFailures(Email email) {
        data.deleteByEmail(email.value());
    }
}
