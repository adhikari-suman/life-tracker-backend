package com.lifetracker.application.labeling;

/** No such posting in this Book. */
public class PostingNotFoundException extends RuntimeException {

    public PostingNotFoundException() {
        super("posting not found");
    }
}
