package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.shared.SortOrder;

public record MovieSort(MovieSortField field, SortOrder order) {}