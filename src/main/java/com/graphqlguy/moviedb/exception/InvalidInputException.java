package com.graphqlguy.moviedb.exception;

import lombok.Getter;

@Getter
public class InvalidInputException extends RuntimeException {

    private final String field;

    public InvalidInputException(String field,  String message) {
        super(message);
        this.field = field;
    }
}
