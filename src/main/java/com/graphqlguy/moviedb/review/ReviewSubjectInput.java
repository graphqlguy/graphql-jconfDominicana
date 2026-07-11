package com.graphqlguy.moviedb.review;

/**
 * Maps the @oneOf ReviewSubjectInput: GraphQL guarantees exactly one field is non-null.
 */
public record ReviewSubjectInput(String movieId, String tvShowId) {
}
