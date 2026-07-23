package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/**
 * Wire response for one label. {@code path} is the full chain from the root
 * ({@code food / restaurants / fast food}); a client builds the tree from {@code parentId}.
 */
public record LabelResponse(UUID id, String name, String path, UUID parentId, boolean archived) {
}
