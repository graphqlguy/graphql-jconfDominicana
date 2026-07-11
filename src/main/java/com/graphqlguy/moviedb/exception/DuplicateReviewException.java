package com.graphqlguy.moviedb.exception;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException() {
        super("You have already reviewed this title");
    }
}