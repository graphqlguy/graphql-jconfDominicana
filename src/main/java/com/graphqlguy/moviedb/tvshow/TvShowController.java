package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.person.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TvShowController {

    private final TvShowService tvShowService;

    @QueryMapping
    TvShow tvShow(@Argument Long id) {
        return tvShowService.findById(id);
    }

    @QueryMapping
    TvShowPage tvShows(@Argument Integer page, @Argument Integer size) {
        return tvShowService.findAll(page != null ? page : 0, size != null ? size : 10);
    }

    @BatchMapping(typeName = "TvShow")
    Map<TvShow, Set<Person>> creators(List<TvShow> shows) {
        Set<Long> ids = shows.stream().map(TvShow::getId).collect(Collectors.toSet());
        Map<Long, Set<Person>> byShowId = tvShowService.findCreatorsByShowIds(ids);

        Map<TvShow, Set<Person>> result = new HashMap<>();
        for (TvShow show : shows) {
            result.put(show, byShowId.getOrDefault(show.getId(), Set.of()));
        }
        return result;
    }

    @BatchMapping(typeName = "TvShow")
    Map<TvShow, List<TvShowCast>> cast(List<TvShow> shows) {
        Set<Long> ids = shows.stream().map(TvShow::getId).collect(Collectors.toSet());
        Map<Long, List<TvShowCast>> byShowId = tvShowService.findCastByShowIds(ids);

        Map<TvShow, List<TvShowCast>> result = new HashMap<>();
        for (TvShow show : shows) {
            result.put(show, byShowId.getOrDefault(show.getId(), List.of()));
        }
        return result;
    }

    @BatchMapping(typeName = "TvShow")
    Map<TvShow, List<Episode>> episodes(List<TvShow> shows) {
        Set<Long> ids = shows.stream().map(TvShow::getId).collect(Collectors.toSet());
        Map<Long, List<Episode>> byShowId = tvShowService.findEpisodesByShowIds(ids);

        Map<TvShow, List<Episode>> result = new HashMap<>();
        for (TvShow show : shows) {
            result.put(show, byShowId.getOrDefault(show.getId(), List.of()));
        }
        return result;
    }

}