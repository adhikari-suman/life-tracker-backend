package com.lifetracker.infrastructure.persistence.labeling;

import com.lifetracker.domain.ledger.OwnerId;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read side for label attachments, in batches. Exists so that listing transactions can show each
 * posting's label with one query rather than one per posting.
 *
 * <p>Owner-scoped ({@link OwnerId}) — the ADR-0006 read guard.
 */
@Component
public class PostingLabelQueryService {

    private final PostingLabelJpaData data;

    PostingLabelQueryService(PostingLabelJpaData data) {
        this.data = data;
    }

    /** Label id per posting id, for those postings that have one. Postings with no label are absent. */
    public Map<UUID, UUID> labelIdsByPosting(OwnerId owner, List<UUID> postingIds) {
        if (postingIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> byPosting = new HashMap<>();
        for (PostingLabelEntity entity : data.findByOwnerIdAndPostingIdIn(owner.value(), postingIds)) {
            byPosting.put(entity.getPostingId(), entity.getLabelId());
        }
        return byPosting;
    }
}
