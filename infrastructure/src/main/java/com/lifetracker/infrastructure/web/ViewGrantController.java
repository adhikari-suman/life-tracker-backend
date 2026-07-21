package com.lifetracker.infrastructure.web;

import com.lifetracker.application.sharing.GrantView;
import com.lifetracker.application.sharing.GrantViewCommand;
import com.lifetracker.application.sharing.RevokeView;
import com.lifetracker.application.sharing.RevokeViewCommand;
import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.infrastructure.persistence.sharing.ViewGrantQueryService;
import com.lifetracker.infrastructure.web.dto.GrantViewRequest;
import com.lifetracker.infrastructure.web.dto.ViewGrantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * The owner's named View Grants — who may read their Book. Listing is a read (query service);
 * granting (to an existing User, by email) and revoking are writes (use cases).
 */
@RestController
@RequestMapping("/me/view-grants")
class ViewGrantController {

    private final GrantView grantView;
    private final RevokeView revokeView;
    private final ViewGrantQueryService query;

    ViewGrantController(GrantView grantView, RevokeView revokeView, ViewGrantQueryService query) {
        this.grantView = grantView;
        this.revokeView = revokeView;
        this.query = query;
    }

    @GetMapping
    List<ViewGrantResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return query.findByOwner(AuthPrincipal.userId(jwt)).stream()
                .map(view -> new ViewGrantResponse(view.id(), view.granteeEmail(), view.granteeUserId(), view.createdAt()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ViewGrantResponse grant(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GrantViewRequest request) {
        ViewGrant grant = grantView.execute(new GrantViewCommand(AuthPrincipal.userId(jwt), request.email()));
        return new ViewGrantResponse(
                grant.id().value(),
                grant.granteeEmail().value(),
                grant.granteeId().value(),
                grant.createdAt().atOffset(ZoneOffset.UTC));
    }

    @DeleteMapping("/{grantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID grantId) {
        revokeView.execute(new RevokeViewCommand(AuthPrincipal.userId(jwt), ViewGrantId.of(grantId)));
    }
}
