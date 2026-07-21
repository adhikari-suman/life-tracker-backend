package com.lifetracker.infrastructure.web;

import com.lifetracker.domain.user.UserId;
import com.lifetracker.infrastructure.persistence.user.UserQueryService;
import com.lifetracker.infrastructure.persistence.user.UserView;
import com.lifetracker.infrastructure.web.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The authenticated User themselves. Read-side: a query service, not a use case. */
@RestController
class MeController {

    private final UserQueryService users;

    MeController(UserQueryService users) {
        this.users = users;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserId userId = AuthPrincipal.userId(jwt);
        UserView view = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("authenticated user no longer exists: " + userId.value()));
        return new UserResponse(view.id(), view.email(), view.createdAt());
    }
}
