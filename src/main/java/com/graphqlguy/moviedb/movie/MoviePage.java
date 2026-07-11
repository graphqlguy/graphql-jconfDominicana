package com.graphqlguy.moviedb.movie;

import java.util.List;

public record MoviePage(
        List<Movie> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious
) {}