package com.lifetracker.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserTest {

    private static final Email EMAIL = new Email("sam@example.com");
    // An opaque stand-in for an encoded hash; the domain never inspects its contents.
    private static final PasswordHash HASH = new PasswordHash("$argon2id$v=19$m=19456,t=2,p=1$stub");

    @Test
    void a_registered_user_starts_unverified() {
        User user = User.register(UserId.generate(), EMAIL, HASH);
        assertFalse(user.isEmailVerified());
    }

    @Test
    void identity_is_the_id_not_the_fields() {
        UserId id = UserId.generate();
        User one = User.register(id, EMAIL, HASH);
        User sameIdOtherEmail = User.register(id, new Email("other@example.com"), HASH);

        // Same id -> same User, even though the email differs. Aggregates compare on identity.
        assertEquals(one, sameIdOtherEmail);
        assertEquals(one.hashCode(), sameIdOtherEmail.hashCode());
    }

    @Test
    void different_ids_are_different_users() {
        User one = User.register(UserId.generate(), EMAIL, HASH);
        User two = User.register(UserId.generate(), EMAIL, HASH);
        assertNotEquals(one, two);
    }

    @Test
    void toString_does_not_leak_the_hash() {
        User user = User.register(UserId.generate(), EMAIL, HASH);
        assertFalse(user.toString().contains(HASH.value()));
    }
}
