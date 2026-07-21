package com.lifetracker.application.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory {@link UserRepository} fake. A fake you can assert against beats a mock you have to
 * configure, and it stays readable as the use cases grow.
 */
final class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> byId = new HashMap<>();

    @Override
    public void save(User user) {
        byId.put(user.id(), user);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return byId.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }
}
