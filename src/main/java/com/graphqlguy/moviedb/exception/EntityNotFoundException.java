package com.graphqlguy.moviedb.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {

    private final String entityType;

    public EntityNotFoundException(String entityType, Long id) {
        super("Entity " + entityType + " not found for id: " + id);
        this.entityType = entityType;
    }
}
