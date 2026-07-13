package com.graphqlguy.moviedb.review;

public record ReviewNotification(Review review, Long movieId, Long tvShowId, String title) {

    static ReviewNotification of(Review review) {
        if (review.getMovie() != null) {
            return new ReviewNotification(review, review.getMovie().getId(), null, review.getMovie().getTitle());
        }
        return new ReviewNotification(review, null, review.getTvShow().getId(), review.getTvShow().getTitle());
    }
}
