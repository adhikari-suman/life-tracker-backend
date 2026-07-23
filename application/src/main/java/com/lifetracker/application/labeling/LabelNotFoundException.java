package com.lifetracker.application.labeling;

/** No such label in this Book. */
public class LabelNotFoundException extends RuntimeException {

    public LabelNotFoundException() {
        super("label not found");
    }
}
