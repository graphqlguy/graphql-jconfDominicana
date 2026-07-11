package com.graphqlguy.moviedb.review;

import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.tvshow.TvShow;
import com.graphqlguy.moviedb.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
// The unique constraints back the existsBy... pre-checks in ReviewService: two
// concurrent createReview calls can both pass the check, so the DB must be the
// one to actually enforce "one review per user per title".
@Entity
@Table(name = "reviews", uniqueConstraints = {@UniqueConstraint(name = "uq_review_user_movie", columnNames = {"user_id", "movie_id"}), @UniqueConstraint(name = "uq_review_user_tvshow", columnNames = {"user_id", "tv_show_id"})})
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private int score;
    // Length matches the @Size(max: 2000) in schema.graphqls; the default varchar(255)
    // would reject schema-valid comments at INSERT time.
    @Column(length = 2000)
    private String comment;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_show_id")
    private TvShow tvShow;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }


    public static class ReviewBuilder {
        private Long id;
        private int score;
        private String comment;
        private OffsetDateTime createdAt;
        private AppUser user;
        private Movie movie;
        private TvShow tvShow;

        ReviewBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder score(final int score) {
            this.score = score;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder comment(final String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder createdAt(final OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder user(final AppUser user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder movie(final Movie movie) {
            this.movie = movie;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Review.ReviewBuilder tvShow(final TvShow tvShow) {
            this.tvShow = tvShow;
            return this;
        }

        public Review build() {
            return new Review(this.id, this.score, this.comment, this.createdAt, this.user, this.movie, this.tvShow);
        }

        @Override
        public String toString() {
            return "Review.ReviewBuilder(id=" + this.id + ", score=" + this.score + ", comment=" + this.comment + ", createdAt=" + this.createdAt + ", user=" + this.user + ", movie=" + this.movie + ", tvShow=" + this.tvShow + ")";
        }
    }

    public static Review.ReviewBuilder builder() {
        return new Review.ReviewBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public int getScore() {
        return this.score;
    }

    public String getComment() {
        return this.comment;
    }

    public OffsetDateTime getCreatedAt() {
        return this.createdAt;
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

    public void setScore(final int score) {
        this.score = score;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
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

    public Review() {
    }

    public Review(final Long id, final int score, final String comment, final OffsetDateTime createdAt, final AppUser user, final Movie movie, final TvShow tvShow) {
        this.id = id;
        this.score = score;
        this.comment = comment;
        this.createdAt = createdAt;
        this.user = user;
        this.movie = movie;
        this.tvShow = tvShow;
    }
}
