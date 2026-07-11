package com.graphqlguy.moviedb.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieDetails(
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount) {}