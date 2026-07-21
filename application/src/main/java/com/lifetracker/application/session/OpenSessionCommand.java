package com.lifetracker.application.session;

import com.lifetracker.domain.user.UserId;

/** Input to {@link OpenSession}: the authenticated User and a label for the device logging in. */
public record OpenSessionCommand(UserId userId, String deviceLabel) {
}
