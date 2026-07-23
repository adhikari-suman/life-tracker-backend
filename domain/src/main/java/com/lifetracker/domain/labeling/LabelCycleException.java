package com.lifetracker.domain.labeling;

/** A label cannot be moved under itself or under one of its own descendants. */
public class LabelCycleException extends RuntimeException {

    public LabelCycleException() {
        super("a label cannot be moved beneath itself or its own descendant");
    }
}
