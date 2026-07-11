package com.graphqlguy.moviedb.person;

import org.springframework.graphql.data.ArgumentValue;

public record UpdatePersonInput(
        Long id,
        ArgumentValue<String> name,
        ArgumentValue<Integer> birthYear,
        ArgumentValue<String> countryCode) {
}
