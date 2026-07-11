package com.graphqlguy.moviedb.review;

import com.graphqlguy.moviedb.config.LatencySimulator;
import com.graphqlguy.moviedb.exception.DuplicateReviewException;
import com.graphqlguy.moviedb.exception.EntityNotFoundException;
import com.graphqlguy.moviedb.exception.InvalidInputException;
import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.movie.MovieRepository;
import com.graphqlguy.moviedb.tvshow.TvShow;
import com.graphqlguy.moviedb.tvshow.TvShowRepository;
import com.graphqlguy.moviedb.user.AppUser;
import com.graphqlguy.moviedb.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final UserRepository userRepository;
    private final LatencySimulator latencySimulator;

    public ReviewService(final ReviewRepository reviewRepository, final MovieRepository movieRepository,
                         final TvShowRepository tvShowRepository, final UserRepository userRepository, final LatencySimulator latencySimulator) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
        this.tvShowRepository = tvShowRepository;
        this.userRepository = userRepository;        this.latencySimulator = latencySimulator;
    }

    @Transactional
    public Review createReview(CreateReviewInput input, String username) {
        latencySimulator.pause();
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("No user record found for: " + username));

        Review.ReviewBuilder review = Review.builder()
                .user(user)
                .score(input.score())
                .comment(input.comment());

        // Exactly one of movieId/tvShowId is expected to be set
        if (input.subject().movieId() != null) {
            Long movieId = parseId(input.subject().movieId(), "movieId");
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new EntityNotFoundException("Movie", movieId));
            if (reviewRepository.existsByMovieIdAndUserId(movieId, user.getId())) {
                throw new DuplicateReviewException();
            }
            review.movie(movie);
        } else {
            Long tvShowId = parseId(input.subject().tvShowId(), "tvShowId");
            TvShow tvShow = tvShowRepository.findById(tvShowId)
                    .orElseThrow(() -> new EntityNotFoundException("TvShow", tvShowId));
            if (reviewRepository.existsByTvShowIdAndUserId(tvShowId, user.getId())) {
                throw new DuplicateReviewException();
            }
            review.tvShow(tvShow);
        }

        Review saved;
        try {
            saved = reviewRepository.save(review.build());
        } catch (DataIntegrityViolationException e) {
            // Two concurrent createReview calls can both pass the existsBy pre-check
            // above; the unique constraint on the reviews table catches the loser here.
            throw new DuplicateReviewException();
        }
        return saved;
    }

    private static Long parseId(String rawId, String field) {
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            // IDs arrive as strings, so garbage like "abc" reaches us here;
            // classify it as bad input rather than an unexpected 500.
            throw new InvalidInputException(field, "must be a numeric ID");
        }
    }

    @Transactional
    public DeleteReviewResponse deleteReview(Long reviewId) {
        latencySimulator.pause();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));

        reviewRepository.delete(review);
        return new DeleteReviewResponse(true, reviewId);
    }
}