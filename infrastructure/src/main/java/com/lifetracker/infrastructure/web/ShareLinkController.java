package com.lifetracker.infrastructure.web;

import com.lifetracker.application.sharing.CreateShareLink;
import com.lifetracker.application.sharing.CreateShareLinkResult;
import com.lifetracker.application.sharing.RevokeShareLink;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.infrastructure.persistence.sharing.ShareLinkQueryService;
import com.lifetracker.infrastructure.web.dto.ShareLinkResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;

/**
 * The owner's single anonymous Share Link. Turning it on is idempotent (201 new / 200 existing);
 * getting it re-copies the live URL (404 when off); deleting it burns the token.
 *
 * <p>Status for the create is set on the response directly rather than via {@code ResponseEntity}
 * — the architecture test forbids any {@code *Entity}-named type outside the persistence package.
 */
@RestController
@RequestMapping("/me/share-link")
class ShareLinkController {

    private final CreateShareLink createShareLink;
    private final RevokeShareLink revokeShareLink;
    private final ShareLinkQueryService query;
    private final String baseUrl;

    ShareLinkController(CreateShareLink createShareLink, RevokeShareLink revokeShareLink,
                        ShareLinkQueryService query,
                        @Value("${app.share.base-url:https://life-tracker.example/shared/}") String baseUrl) {
        this.createShareLink = createShareLink;
        this.revokeShareLink = revokeShareLink;
        this.query = query;
        this.baseUrl = baseUrl;
    }

    @GetMapping
    ShareLinkResponse get(@AuthenticationPrincipal Jwt jwt) {
        UserId owner = AuthPrincipal.userId(jwt);
        return query.findByOwner(owner)
                .map(view -> new ShareLinkResponse(url(view.token()), view.createdAt()))
                .orElseThrow(ShareLinkNotFoundException::new);
    }

    @PostMapping
    ShareLinkResponse create(@AuthenticationPrincipal Jwt jwt, HttpServletResponse response) {
        CreateShareLinkResult result = createShareLink.execute(AuthPrincipal.userId(jwt));
        response.setStatus((result.created() ? HttpStatus.CREATED : HttpStatus.OK).value());
        return new ShareLinkResponse(
                url(result.shareLink().token().value()),
                result.shareLink().createdAt().atOffset(ZoneOffset.UTC));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal Jwt jwt) {
        revokeShareLink.execute(AuthPrincipal.userId(jwt));
    }

    private String url(String token) {
        return baseUrl + token;
    }
}
