package com.graphqlguy.moviedb.person;

// Partial-update input: null means "leave the field unchanged".
public record UpdatePersonInput(
        Long id,
        String name,
        Integer birthYear,
        String countryCode) {
}
