package com.graphqlguy.moviedb.tmdb;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TmdbController {

    private final TmdbService tmdbService;

    @QueryMapping
    List<TmdbResult> tmdbSearch(@Argument String title) {
        return tmdbService.search(title);
    }
}
