package com.graphqlguy.moviedb.tvshow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findByTvShowIdInOrderBySeasonNumberAscEpisodeNumberAsc(Set<Long> tvShowIds);
}