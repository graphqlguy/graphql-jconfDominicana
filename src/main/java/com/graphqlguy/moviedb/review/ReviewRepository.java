package com.graphqlguy.moviedb.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select r from Review r join fetch r.user where r.movie.id in :movieIds order by r.createdAt desc")
    List<Review> findWithUserByMovieIdIn(@Param("movieIds") List<Long> movieIds);

    @Query("select r from Review r join fetch r.user where r.tvShow.id in :tvShowIds order by r.createdAt desc")
    List<Review> findWithUserByTvShowIdIn(@Param("tvShowIds") List<Long> tvShowIds);

    @Query("""
            select new com.graphqlguy.moviedb.review.MovieReviewCount(r.movie.id, count(r))
            from Review r where r.movie.id in :movieIds group by r.movie.id""")
    List<MovieReviewCount> countByMovieIdIn(@Param("movieIds") List<Long> movieIds);

    boolean existsByMovieIdAndUserId(Long movieId, Long userId);

    void deleteByMovieId(Long movieId);

    boolean existsByTvShowIdAndUserId(Long tvShowId, Long userId);
}