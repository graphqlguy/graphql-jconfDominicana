package com.graphqlguy.moviedb.watchlist;

/**
 * Maps the @oneOf WatchlistSubjectInput: GraphQL guarantees exactly one field is non-null.
 */
public record WatchlistSubjectInput(String movieId, String tvShowId) {
}
