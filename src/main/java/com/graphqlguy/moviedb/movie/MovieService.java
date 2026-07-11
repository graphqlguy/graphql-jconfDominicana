package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.config.LatencySimulator;
import com.graphqlguy.moviedb.exception.EntityNotFoundException;
import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.review.ReviewRepository;
import com.graphqlguy.moviedb.shared.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final LatencySimulator latencySimulator;

    public MovieService(final MovieRepository movieRepository, final ReviewRepository reviewRepository, final LatencySimulator latencySimulator) {
        this.movieRepository = movieRepository;
        this.reviewRepository = reviewRepository;        this.latencySimulator = latencySimulator;
    }


    List<Movie> findAll() {
        latencySimulator.pause();
        return movieRepository.findAll();
    }

    Movie findById(final Long id) {
        latencySimulator.pause();
        return movieRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Movie", id));
    }

    public List<Movie> searchByTitle(final String title) {
        latencySimulator.pause();
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    public MoviePage findMovies(MovieFilter filter, int page, int size, MovieSort sort) {
        latencySimulator.pause();
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<Movie> result = movieRepository.findWithFilters(
                filter != null ? filter.genre() : null,
                filter != null ? filter.minRating() : null,
                filter != null ? filter.maxRating() : null,
                filter != null ? filter.minYear() : null,
                filter != null ? filter.maxYear() : null,
                filter != null ? filter.titleContains() : null,
                pageable
        );
        return new MoviePage(
                result.getContent(), result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize(), result.isFirst(), result.isLast(),
                result.hasNext(), result.hasPrevious()
        );
    }

    private Sort buildSort(MovieSort sort) {
        if (sort == null || sort.field() == null) return Sort.by("releaseYear").descending();
        String field = switch (sort.field()) {
            case TITLE -> "title";
            case RELEASE_YEAR -> "releaseYear";
            case RATING -> "rating";
            case RUNTIME -> "runtime";
        };
        return sort.order() == SortOrder.ASC ? Sort.by(field).ascending() : Sort.by(field).descending();
    }

    @Transactional
    Movie createMovie(final CreateMovieInput input) {
        latencySimulator.pause();
        log.info("Creating movie {}", input.title());
        return movieRepository.save(Movie.builder()
                .title(input.title()).releaseYear(input.releaseYear()).genre(input.genre())
                .rating(input.rating()).runtime(input.runtime()).plot(input.plot())
                .posterUrl(input.posterUrl()).tmdbId(input.tmdbId())
                .build());
    }

    @Transactional
    Movie updateMovie(final UpdateMovieInput input) {
        latencySimulator.pause();
        log.info("Updating movie {}", input.id());
        final Movie movie = movieRepository.findById(input.id())
                .orElseThrow(() -> new EntityNotFoundException("Movie", input.id()));

        applyIfPresent(input.title(), movie::setTitle);
        applyIfPresent(input.releaseYear(), movie::setReleaseYear);
        applyIfPresent(input.genre(), movie::setGenre);
        applyIfPresent(input.rating(), movie::setRating);
        applyIfPresent(input.runtime(), movie::setRuntime);
        applyIfPresent(input.plot(), movie::setPlot);
        applyIfPresent(input.posterUrl(), movie::setPosterUrl);
        applyIfPresent(input.tmdbId(), movie::setTmdbId);

        return movieRepository.save(movie);
    }

    private <T> void applyIfPresent(final T value, final Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    @Transactional
    DeleteMovieResponse deleteMovie(final Long id) {
        latencySimulator.pause();
        log.info("Delete movie with id {}", id);
        if (!movieRepository.existsById(id)) {
            return new DeleteMovieResponse(false, "Movie not found for Id = " + id, null);
        }

        reviewRepository.deleteByMovieId(id);
        movieRepository.deleteById(id);
        return new DeleteMovieResponse(true, "Movie deleted successfully", id);
    }

    List<Movie> findByIds(final List<Long> ids) {
        latencySimulator.pause();
        return movieRepository.findAllById(ids);
    }

    // One SQL query per movie: fine for a single movie, and deliberately naive
    // for lists. The N+1 class replaces this with batch loading.
    public List<Person> getDirectors(final Movie movie) {
        latencySimulator.pause();
        return movieRepository.findWithDirectorsById(movie.getId())
                .map(Movie::getDirectors)
                .orElseGet(List::of);
    }
}
