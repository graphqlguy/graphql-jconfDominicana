package com.graphqlguy.moviedb.tmdb;

public record TmdbResult(
        Integer tmdbId,
        String title,
        Integer releaseYear,
        String overview,
        String posterUrl,
        Double rating) {}
