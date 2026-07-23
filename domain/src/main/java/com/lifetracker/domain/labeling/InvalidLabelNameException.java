package com.lifetracker.domain.labeling;

/** A label name that is blank or too long. */
public class InvalidLabelNameException extends RuntimeException {

    public InvalidLabelNameException(String message) {
        super(message);
    }
}
