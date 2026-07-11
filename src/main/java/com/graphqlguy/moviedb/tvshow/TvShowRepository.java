package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TvShowRepository extends JpaRepository<TvShow, Long> {

    List<TvShow> findByTitleContainingIgnoreCase(String title);

    List<TvShow> findByCreatorsContaining(Person person);

    boolean existsByCreatorsContaining(Person person);

    @Query("select distinct s from TvShow s left join fetch s.creators where s.id in :ids")
    List<TvShow> findWithCreatorsByIdIn(@Param("ids") Set<Long> ids);

    @Query("select distinct s from TvShow s left join fetch s.creators where s in " +
            "(select s2 from TvShow s2 join s2.creators c where c.id in :personIds)")
    List<TvShow> findAllWithCreatorsByCreatorIdIn(@Param("personIds") List<Long> personIds);
}