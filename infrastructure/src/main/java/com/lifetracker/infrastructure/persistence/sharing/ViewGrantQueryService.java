package com.lifetracker.infrastructure.persistence.sharing;

import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

/** Read side for an owner's View Grants ({@code GET /me/view-grants}). */
@Component
public class ViewGrantQueryService {

    private final ViewGrantJpaData data;

    ViewGrantQueryService(ViewGrantJpaData data) {
        this.data = data;
    }

    public List<ViewGrantView> findByOwner(UserId ownerId) {
        return data.findByOwnerUserId(ownerId.value()).stream()
                .map(e -> new ViewGrantView(e.getId(), e.getGranteeEmail(), e.getGranteeUserId(), e.getCreatedAt()))
                .toList();
    }
}
