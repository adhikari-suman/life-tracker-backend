package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/** Body of {@code POST /labels}: a name, and the parent to nest under (null for a root label). */
public record CreateLabelRequest(String name, UUID parentId) {
}
