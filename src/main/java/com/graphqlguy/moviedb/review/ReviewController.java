package com.graphqlguy.moviedb.review;

import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.tvshow.TvShow;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Validated
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final ReviewPublisher reviewPublisher;

    @BatchMapping
    Map<Movie, List<Review>> reviews(List<Movie> movies) {
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        Map<Long, List<Review>> reviewsByMovieId = reviewRepository.findWithUserByMovieIdIn(movieIds)
                .stream().collect(Collectors.groupingBy(review -> review.getMovie().getId()));
        return movies.stream()
                .collect(Collectors.toMap(movie -> movie,
                        movie -> reviewsByMovieId.getOrDefault(movie.getId(), List.of())));
    }

    @BatchMapping(typeName = "Movie", field = "reviewCount")
    Map<Movie, Integer> reviewCount(List<Movie> movies) {
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        Map<Long, Long> countsByMovieId = reviewRepository.countByMovieIdIn(movieIds)
                .stream().collect(Collectors.toMap(MovieReviewCount::movieId, MovieReviewCount::count));
        return movies.stream()
                .collect(Collectors.toMap(movie -> movie,
                        movie -> countsByMovieId.getOrDefault(movie.getId(), 0L).intValue()));
    }

    @BatchMapping(typeName = "TvShow", field = "reviews")
    Map<TvShow, List<Review>> tvShowReviews(List<TvShow> tvShows) {
        List<Long> tvShowIds = tvShows.stream().map(TvShow::getId).toList();
        Map<Long, List<Review>> reviewsByTvShowId = reviewRepository.findWithUserByTvShowIdIn(tvShowIds)
                .stream().collect(Collectors.groupingBy(review -> review.getTvShow().getId()));
        return tvShows.stream()
                .collect(Collectors.toMap(tvShow -> tvShow,
                        tvShow -> reviewsByTvShowId.getOrDefault(tvShow.getId(), List.of())));
    }

    @MutationMapping
    Review createReview(@Argument @Valid CreateReviewInput input, Principal principal) {
        return reviewService.createReview(input, principal.getName());
    }

    @SubscriptionMapping
    Flux<ReviewNotification> reviewAdded(@Argument Long movieId) {
        return reviewPublisher.flux()
                .filter(notification -> movieId == null || movieId.equals(notification.movieId()));
    }

    @MutationMapping
    DeleteReviewResponse deleteReview(@Argument Long id, Principal principal) {
        return reviewService.deleteReview(id, principal.getName());
    }
}