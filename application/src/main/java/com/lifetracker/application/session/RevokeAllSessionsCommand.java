package com.lifetracker.application.session;

import com.lifetracker.domain.user.UserId;

/** Input to {@link RevokeAllSessions}: the User whose Sessions all end. */
public record RevokeAllSessionsCommand(UserId userId) {
}
