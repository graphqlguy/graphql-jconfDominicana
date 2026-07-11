package com.graphqlguy.moviedb.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbSearchResponse(List<Result> results) {

    public record Result(
            Integer id,
            String title,
            @JsonProperty("release_date") String releaseDate,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("vote_average") Double voteAverage) {}
}