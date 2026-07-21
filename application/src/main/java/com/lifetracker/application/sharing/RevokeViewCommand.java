package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.domain.user.UserId;

/** Input to {@link RevokeView}: the owner, and the grant to revoke (which must be theirs). */
public record RevokeViewCommand(UserId ownerId, ViewGrantId grantId) {
}
