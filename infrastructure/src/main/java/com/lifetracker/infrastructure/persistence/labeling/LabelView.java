package com.lifetracker.infrastructure.persistence.labeling;

import java.util.UUID;

/**
 * A flat read view of a label. {@code path} is the full chain from the root
 * ({@code food / restaurants / fast food}) — sibling names are unique only within a parent, so the
 * leaf name alone can be ambiguous and the path is what disambiguates it.
 */
public record LabelView(UUID id, String name, String path, UUID parentId, boolean archived) {
}
