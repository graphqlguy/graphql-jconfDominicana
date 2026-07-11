package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.shared.Genre;

// Partial-update input: null means "leave the field unchanged".
public record UpdateMovieInput(
        Long id,
        String title,
        Integer releaseYear,
        Genre genre,
        Double rating,
        Integer runtime,
        String plot,
        String posterUrl,
        Integer tmdbId) {
}
