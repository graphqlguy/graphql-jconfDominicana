package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.shared.Genre;

public record CreateMovieInput(
        String title,
        Integer releaseYear,
        Genre genre,
        Double rating,
        Integer runtime,
        String plot,
        String posterUrl,
        Integer tmdbId) {
}
