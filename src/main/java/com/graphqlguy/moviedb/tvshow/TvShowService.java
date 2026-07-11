package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.config.LatencySimulator;
import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TvShowService {

    private final TvShowRepository tvShowRepository;
    private final TvShowCastRepository tvShowCastRepository;
    private final EpisodeRepository episodeRepository;
    private final LatencySimulator latencySimulator;

    public TvShow findById(Long id) {
        latencySimulator.pause();
        return tvShowRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TvShow", id));
    }

    public TvShowPage findAll(int page, int size) {
        latencySimulator.pause();
        Page<TvShow> result = tvShowRepository.findAll(
                PageRequest.of(page, size, Sort.by("startYear").descending().and(Sort.by("id"))));
        return new TvShowPage(result.getContent(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize());
    }

    public List<TvShow> searchByTitle(String title) {
        latencySimulator.pause();
        return tvShowRepository.findByTitleContainingIgnoreCase(title);
    }

    public Map<Long, Set<Person>> findCreatorsByShowIds(final Set<Long> showIds) {
        log.info("Batch fetching creators for {} TV shows", showIds.size());
        latencySimulator.pause();
        return tvShowRepository.findWithCreatorsByIdIn(showIds).stream()
                .collect(Collectors.toMap(TvShow::getId, TvShow::getCreators));
    }

    public Map<Long, List<TvShowCast>> findCastByShowIds(final Set<Long> showIds) {
        log.info("Batch fetching cast for {} TV shows", showIds.size());
        latencySimulator.pause();
        return tvShowCastRepository.findWithPersonByTvShowIdIn(showIds).stream()
                .collect(Collectors.groupingBy(cast -> cast.getTvShow().getId()));
    }

    public Map<Long, List<Episode>> findEpisodesByShowIds(final Set<Long> showIds) {
        log.info("Batch fetching episodes for {} TV shows", showIds.size());
        latencySimulator.pause();
        return episodeRepository.findByTvShowIdInOrderBySeasonNumberAscEpisodeNumberAsc(showIds).stream()
                .collect(Collectors.groupingBy(episode -> episode.getTvShow().getId()));
    }
}
