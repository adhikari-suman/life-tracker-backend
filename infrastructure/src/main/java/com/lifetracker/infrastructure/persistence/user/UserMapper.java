package com.lifetracker.infrastructure.persistence.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.PasswordHash;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;

/**
 * Converts between the domain {@link User} and the {@link UserEntity}. Plain static methods — no
 * MapStruct, no reflection — because reassembling a credential must be readable by eye.
 */
final class UserMapper {

    private UserMapper() {
    }

    static UserEntity toEntity(User user) {
        return new UserEntity(
                user.id().value(),
                user.email().value(),
                user.passwordHash().value(),
                user.isEmailVerified());
    }

    static User toDomain(UserEntity entity) {
        return User.rehydrate(
                new UserId(entity.getId()),
                new Email(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                entity.isEmailVerified());
    }
}
