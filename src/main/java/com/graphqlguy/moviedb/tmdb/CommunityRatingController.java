package com.graphqlguy.moviedb.tmdb;

import com.graphqlguy.moviedb.movie.Movie;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CommunityRatingController {

    private final TmdbService tmdbService;

    @BatchMapping(typeName = "Movie")
    Map<Movie, CommunityRating> communityRating(List<Movie> movies) {

        List<Movie> moviesWithTmdb = movies.stream()
                .filter(m -> m.getTmdbId() != null)
                .toList();

        if (moviesWithTmdb.isEmpty()) return Collections.emptyMap();

        List<Integer> tmdbIds = moviesWithTmdb.stream().map(Movie::getTmdbId).toList();
        Map<Integer, CommunityRating> ratings = tmdbService.fetchMovieRatings(tmdbIds);

        Map<Movie, CommunityRating> result = new HashMap<>();
        for (Movie movie : moviesWithTmdb) {
            CommunityRating rating = ratings.get(movie.getTmdbId());
            if (rating != null) {
                result.put(movie, rating);
            }
        }
        return result;
    }
}
