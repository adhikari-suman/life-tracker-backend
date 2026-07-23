package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/**
 * Body of {@code PATCH /labels/{labelId}}: rename, reparent, or archive. Every field is optional and
 * an omitted one is left alone.
 *
 * <p>Deliberately NOT a record. {@code parentId} has three meanings, not two — absent means "leave the
 * parent alone", explicit {@code null} means "move this to the root", and a uuid means "move it
 * there". A record component cannot tell absent from null, so presence is tracked in the setter, which
 * Jackson calls only for keys that actually appear in the body.
 */
public class UpdateLabelRequest {

    private String name;
    private UUID parentId;
    private boolean parentIdPresent;
    private Boolean archived;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    /** Whether the caller mentioned {@code parentId} at all — the difference between "leave" and "move to root". */
    public boolean isParentIdPresent() {
        return parentIdPresent;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
}
