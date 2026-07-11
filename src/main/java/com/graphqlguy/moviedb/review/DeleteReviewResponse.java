package com.graphqlguy.moviedb.review;

public record DeleteReviewResponse(boolean success, Long deletedId) {}