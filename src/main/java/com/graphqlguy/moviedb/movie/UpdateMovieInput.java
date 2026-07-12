package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.shared.Genre;
import org.springframework.graphql.data.ArgumentValue;

public record UpdateMovieInput(
        Long id,
        ArgumentValue<String> title,
        ArgumentValue<Integer> releaseYear,
        ArgumentValue<Genre> genre,
        ArgumentValue<Double> rating,
        ArgumentValue<Integer> runtime,
        ArgumentValue<String> plot,
        ArgumentValue<String> posterUrl,
        ArgumentValue<Integer> tmdbId) {
}
