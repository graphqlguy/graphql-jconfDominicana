package com.graphqlguy.moviedb.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

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
