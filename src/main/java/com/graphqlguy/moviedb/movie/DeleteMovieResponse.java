package com.graphqlguy.moviedb.movie;

public record DeleteMovieResponse(boolean success, String message, Long deletedId) {
}
