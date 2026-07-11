package com.graphqlguy.moviedb.tvshow;

import java.util.List;

public record TvShowPage(
        List<TvShow> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {}
