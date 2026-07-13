package com.graphqlguy.moviedb.watchlist;

import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.tvshow.TvShow;
import com.graphqlguy.moviedb.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
// One entry per user per title; the DB enforces it. The existsBy pre-check in the
// service can race under concurrency, exactly as with reviews, so the constraint
// is the real guard.
@Table(name = "watchlist_items", uniqueConstraints = {
        @UniqueConstraint(name = "uq_watchlist_user_movie", columnNames = {"user_id", "movie_id"}),
        @UniqueConstraint(name = "uq_watchlist_user_tvshow", columnNames = {"user_id", "tv_show_id"})
})
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // Exactly one of movie/tvShow is set, mirroring the @oneOf input on the GraphQL side.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_show_id")
    private TvShow tvShow;
}
