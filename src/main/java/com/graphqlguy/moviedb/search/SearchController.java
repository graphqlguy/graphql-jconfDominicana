package com.graphqlguy.moviedb.search;

import com.graphqlguy.moviedb.movie.MovieService;
import com.graphqlguy.moviedb.person.PersonService;
import com.graphqlguy.moviedb.shared.SearchResult;
import com.graphqlguy.moviedb.tvshow.TvShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final MovieService movieService;
    private final PersonService personService;
    private final TvShowService tvShowService;

    @QueryMapping
    List<SearchResult> search(@Argument String query) {
        List<SearchResult> results = new ArrayList<>();
        results.addAll(movieService.searchByTitle(query));
        results.addAll(personService.searchByName(query));
        results.addAll(tvShowService.searchByTitle(query));
        return results;
    }
}
