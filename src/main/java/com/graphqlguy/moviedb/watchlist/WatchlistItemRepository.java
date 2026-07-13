package com.graphqlguy.moviedb.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    // Fetch the movie and TV show up front: the WatchlistItem.content resolver runs
    // after the service transaction closes, so a lazy load there would fail.
    @Query("""
            select w from WatchlistItem w
            left join fetch w.movie
            left join fetch w.tvShow
            where w.user.id = :userId
            order by w.id
            """)
    List<WatchlistItem> findWithContentByUserId(Long userId);

    // Same eager fetch for a single item, used when a mutation returns one whose
    // content must then be resolved.
    @Query("""
            select w from WatchlistItem w
            left join fetch w.movie
            left join fetch w.tvShow
            where w.id = :id
            """)
    Optional<WatchlistItem> findWithContentById(Long id);

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    boolean existsByUserIdAndTvShowId(Long userId, Long tvShowId);
}
