package com.graphqlguy.moviedb.tvshow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TvShowCastRepository extends JpaRepository<TvShowCast, Long> {

    boolean existsByPersonId(Long personId);

    void deleteByPersonId(Long personId);

    @Query("select tc from TvShowCast tc join fetch tc.person where tc.tvShow.id in :ids")
    List<TvShowCast> findWithPersonByTvShowIdIn(@Param("ids") Set<Long> ids);

    @Query("select tc from TvShowCast tc join fetch tc.tvShow where tc.person.id = :personId")
    List<TvShowCast> findWithTvShowByPersonId(@Param("personId") Long personId);

    @Query("select tc from TvShowCast tc join fetch tc.tvShow where tc.person.id in :personIds")
    List<TvShowCast> findWithTvShowByPersonIdIn(@Param("personIds") List<Long> personIds);
}