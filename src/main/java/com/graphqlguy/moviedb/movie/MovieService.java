package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.config.LatencySimulator;
import com.graphqlguy.moviedb.exception.EntityNotFoundException;
import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.review.ReviewRepository;
import com.graphqlguy.moviedb.shared.SortOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.ArgumentValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final LatencySimulator latencySimulator;


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
        applyIfProvided(input.rating(), movie::setRating);
        applyIfProvided(input.runtime(), movie::setRuntime);
        applyIfProvided(input.plot(), movie::setPlot);
        applyIfProvided(input.posterUrl(), movie::setPosterUrl);
        applyIfProvided(input.tmdbId(), movie::setTmdbId);

        return movieRepository.save(movie);
    }

    // Required fields: a provided value replaces the old one; null or omitted leaves it unchanged.
    private <T> void applyIfPresent(final ArgumentValue<T> arg, final Consumer<T> setter) {
        if (arg.isPresent()) {
            setter.accept(arg.value());
        }
    }

    // Optional fields: an explicit null clears the value; an omitted field leaves it unchanged.
    private <T> void applyIfProvided(final ArgumentValue<T> arg, final Consumer<T> setter) {
        if (!arg.isOmitted()) {
            setter.accept(arg.value());
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
