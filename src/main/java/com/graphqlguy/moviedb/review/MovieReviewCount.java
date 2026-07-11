package com.graphqlguy.moviedb.review;

/**
 * Projection for the grouped review COUNT query.
 */
public record MovieReviewCount(Long movieId, long count) {
}
