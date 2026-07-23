package com.lifetracker.infrastructure.persistence.labeling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code posting_labels}. Internal to the adapter and query service. */
interface PostingLabelJpaData extends JpaRepository<PostingLabelEntity, UUID> {

    Optional<PostingLabelEntity> findByOwnerIdAndPostingId(UUID ownerId, UUID postingId);

    boolean existsByOwnerIdAndLabelId(UUID ownerId, UUID labelId);

    List<PostingLabelEntity> findByOwnerIdAndPostingIdIn(UUID ownerId, List<UUID> postingIds);

    void deleteByOwnerIdAndPostingId(UUID ownerId, UUID postingId);
}
