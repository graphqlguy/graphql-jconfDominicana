package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.shared.Genre;

public record MovieFilter(
        Genre genre,
        Double minRating,
        Double maxRating,
        Integer minYear,
        Integer maxYear,
        String titleContains
) {}