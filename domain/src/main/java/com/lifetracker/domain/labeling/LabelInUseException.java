package com.lifetracker.domain.labeling;

/**
 * Deleting is for tidying, never for destroying history (ADR-0015), so it is refused while postings
 * are still tagged. Archive the label instead, or retag those postings first.
 */
public class LabelInUseException extends RuntimeException {

    public LabelInUseException() {
        super("postings are still tagged with this label; archive it, or retag them first");
    }
}
