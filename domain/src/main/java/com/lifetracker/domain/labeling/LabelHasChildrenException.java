package com.lifetracker.domain.labeling;

/** A label with children cannot be deleted; the children would be orphaned. */
public class LabelHasChildrenException extends RuntimeException {

    public LabelHasChildrenException() {
        super("this label still has children; delete or move them first");
    }
}
