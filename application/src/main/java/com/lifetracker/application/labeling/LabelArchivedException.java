package com.lifetracker.application.labeling;

/**
 * An archived label is retired from use: it still reports whatever was already tagged with it, but it
 * cannot be newly applied. Restore it first (ADR-0015).
 */
public class LabelArchivedException extends RuntimeException {

    public LabelArchivedException() {
        super("that label is archived and cannot be newly applied");
    }
}
