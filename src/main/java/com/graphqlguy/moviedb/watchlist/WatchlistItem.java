package com.graphqlguy.moviedb.watchlist;

import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.tvshow.TvShow;
import com.graphqlguy.moviedb.user.AppUser;
import jakarta.persistence.*;

@Entity
// One entry per user per title; the DB enforces it. The existsBy pre-check in the
// service can race under concurrency, so the constraint is the real guard.
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistItem other = (WatchlistItem) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class WatchlistItemBuilder {
        private Long id;
        private WatchStatus status;
        private AppUser user;
        private Movie movie;
        private TvShow tvShow;

        WatchlistItemBuilder() {
        }

        public WatchlistItem.WatchlistItemBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        public WatchlistItem.WatchlistItemBuilder status(final WatchStatus status) {
            this.status = status;
            return this;
        }

        public WatchlistItem.WatchlistItemBuilder user(final AppUser user) {
            this.user = user;
            return this;
        }

        public WatchlistItem.WatchlistItemBuilder movie(final Movie movie) {
            this.movie = movie;
            return this;
        }

        public WatchlistItem.WatchlistItemBuilder tvShow(final TvShow tvShow) {
            this.tvShow = tvShow;
            return this;
        }

        public WatchlistItem build() {
            return new WatchlistItem(this.id, this.status, this.user, this.movie, this.tvShow);
        }

        @Override
        public String toString() {
            return "WatchlistItem.WatchlistItemBuilder(id=" + this.id + ", status=" + this.status + ", user=" + this.user + ", movie=" + this.movie + ", tvShow=" + this.tvShow + ")";
        }
    }

    public static WatchlistItem.WatchlistItemBuilder builder() {
        return new WatchlistItem.WatchlistItemBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public WatchStatus getStatus() {
        return this.status;
    }

    public AppUser getUser() {
        return this.user;
    }

    public Movie getMovie() {
        return this.movie;
    }

    public TvShow getTvShow() {
        return this.tvShow;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setStatus(final WatchStatus status) {
        this.status = status;
    }

    public void setUser(final AppUser user) {
        this.user = user;
    }

    public void setMovie(final Movie movie) {
        this.movie = movie;
    }

    public void setTvShow(final TvShow tvShow) {
        this.tvShow = tvShow;
    }

    public WatchlistItem() {
    }

    public WatchlistItem(final Long id, final WatchStatus status, final AppUser user, final Movie movie, final TvShow tvShow) {
        this.id = id;
        this.status = status;
        this.user = user;
        this.movie = movie;
        this.tvShow = tvShow;
    }
}
