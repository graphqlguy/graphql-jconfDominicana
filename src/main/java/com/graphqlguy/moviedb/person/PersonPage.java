package com.graphqlguy.moviedb.person;

import java.util.List;

public record PersonPage(
        List<Person> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {}