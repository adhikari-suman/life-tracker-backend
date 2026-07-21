package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.domain.sharing.ViewGrantRepository;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserRepository;

import java.time.Clock;

/**
 * Grants a named, already-registered User read access to the owner's Book. The grantee is resolved
 * by email; an unknown email is refused ({@link GranteeNotFoundException}), a self-grant is
 * meaningless ({@link CannotShareWithYourselfException}), and a duplicate is rejected
 * ({@link ViewGrantAlreadyExistsException}). Pending-invite-by-email is deferred (ADR-0008).
 */
public final class GrantView {

    private final ViewGrantRepository grants;
    private final UserRepository users;
    private final Clock clock;

    public GrantView(ViewGrantRepository grants, UserRepository users, Clock clock) {
        this.grants = grants;
        this.users = users;
        this.clock = clock;
    }

    public ViewGrant execute(GrantViewCommand command) {
        Email granteeEmail = new Email(command.granteeEmail()); // InvalidEmailException -> 422
        User grantee = users.findByEmail(granteeEmail).orElseThrow(GranteeNotFoundException::new);

        if (grantee.id().equals(command.ownerId())) {
            throw new CannotShareWithYourselfException();
        }
        if (grants.findByOwnerIdAndGranteeId(command.ownerId(), grantee.id()).isPresent()) {
            throw new ViewGrantAlreadyExistsException();
        }

        ViewGrant grant = ViewGrant.create(
                ViewGrantId.generate(), command.ownerId(), grantee.id(), grantee.email(), clock.instant());
        grants.save(grant);
        return grant;
    }
}
