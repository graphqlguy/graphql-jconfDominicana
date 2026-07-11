package com.graphqlguy.moviedb.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewInput(
        @NotNull ReviewSubjectInput subject,
        @Min(1) @Max(10) int score,
        @Size(max = 2000) String comment
) {}
