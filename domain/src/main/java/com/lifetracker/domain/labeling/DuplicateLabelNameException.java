package com.lifetracker.domain.labeling;

/** Two siblings cannot share a name, compared case-insensitively. */
public class DuplicateLabelNameException extends RuntimeException {

    public DuplicateLabelNameException(LabelName name) {
        super("a sibling label named '" + name.value() + "' already exists");
    }
}
