package com.lifetracker.application.sharing;

import com.lifetracker.domain.user.UserId;

/** Input to {@link GrantView}: the owner, and the email of the person to grant read access to. */
public record GrantViewCommand(UserId ownerId, String granteeEmail) {
}
