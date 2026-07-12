package com.graphqlguy.moviedb.movie;

import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @QueryMapping
    List<Movie> moviesAll() {
        return movieService.findAll();
    }

    @QueryMapping
    Movie movie(@Argument Long id){
        return movieService.findById(id);
    }

    @QueryMapping
    public MoviePage movies(@Argument MovieFilter filter, @Argument Integer page,
                            @Argument Integer size, @Argument MovieSort sort) {
        return movieService.findMovies(filter, page != null ? page : 0, size != null ? size : 10, sort);
    }

    @QueryMapping
    public DataFetcherResult<List<Movie>> moviesByIds(@Argument List<Long> ids, DataFetchingEnvironment env) {
        List<Movie> found = movieService.findByIds(ids);
        Set<Long> foundIds = found.stream().map(Movie::getId).collect(Collectors.toSet());
        List<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();

        var result = DataFetcherResult.<List<Movie>>newResult().data(found);
        if (!missing.isEmpty()) {
            result.error(GraphqlErrorBuilder.newError(env)
                    .message("Movies not found: " + missing)
                    .errorType(ErrorType.NOT_FOUND)
                    .build());
        }
        return result.build();
    }


    @MutationMapping
    Movie createMovie(@Argument CreateMovieInput input) {
        return movieService.createMovie(input);
    }

    @MutationMapping
    Movie updateMovie(@Argument UpdateMovieInput input) {
        return movieService.updateMovie(input);
    }

    @MutationMapping
    DeleteMovieResponse deleteMovie(@Argument Long id) {
        return movieService.deleteMovie(id);
    }

}
