package com.lifetracker.infrastructure.persistence.sharing;

import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Read side for the owner's Share Link ({@code GET /me/share-link}). */
@Component
public class ShareLinkQueryService {

    private final ShareLinkJpaData data;

    ShareLinkQueryService(ShareLinkJpaData data) {
        this.data = data;
    }

    public Optional<ShareLinkView> findByOwner(UserId ownerId) {
        return data.findByOwnerUserId(ownerId.value())
                .map(e -> new ShareLinkView(e.getToken(), e.getCreatedAt()));
    }
}
