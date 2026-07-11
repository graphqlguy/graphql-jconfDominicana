package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieCastRepository extends JpaRepository<MovieCast, Long> {

    List<MovieCast> findAllByMovieId(Long movieId);

    boolean existsByPerson(Person person);

    void deleteByPerson(Person person);

    @Query("select mc from MovieCast mc join fetch mc.person where mc.movie.id in :ids")
    List<MovieCast> findWithPersonByMovieIdIn(@Param("ids") List<Long> ids);

    @Query("select mc from MovieCast mc join fetch mc.movie where mc.person.id = :personId")
    List<MovieCast> findWithMovieByPersonId(@Param("personId") Long personId);

    @Query("select mc from MovieCast mc join fetch mc.movie where mc.person.id in :personIds")
    List<MovieCast> findWithMovieByPersonIdIn(@Param("personIds") List<Long> personIds);
}
