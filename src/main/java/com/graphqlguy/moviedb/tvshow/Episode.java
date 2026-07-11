package com.graphqlguy.moviedb.tvshow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "episodes")
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tvshow_id", nullable = false)
    private TvShow tvShow;
    @Column(nullable = false)
    private Integer seasonNumber;
    @Column(nullable = false)
    private Integer episodeNumber;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String overview;
    private Integer runtime;
    private Integer airYear;


    public static class EpisodeBuilder {
        private Long id;
        private TvShow tvShow;
        private Integer seasonNumber;
        private Integer episodeNumber;
        private String title;
        private String overview;
        private Integer runtime;
        private Integer airYear;

        EpisodeBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder tvShow(final TvShow tvShow) {
            this.tvShow = tvShow;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder seasonNumber(final Integer seasonNumber) {
            this.seasonNumber = seasonNumber;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder episodeNumber(final Integer episodeNumber) {
            this.episodeNumber = episodeNumber;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder overview(final String overview) {
            this.overview = overview;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder runtime(final Integer runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Episode.EpisodeBuilder airYear(final Integer airYear) {
            this.airYear = airYear;
            return this;
        }

        public Episode build() {
            return new Episode(this.id, this.tvShow, this.seasonNumber, this.episodeNumber, this.title, this.overview, this.runtime, this.airYear);
        }

        @Override
        public String toString() {
            return "Episode.EpisodeBuilder(id=" + this.id + ", tvShow=" + this.tvShow + ", seasonNumber=" + this.seasonNumber + ", episodeNumber=" + this.episodeNumber + ", title=" + this.title + ", overview=" + this.overview + ", runtime=" + this.runtime + ", airYear=" + this.airYear + ")";
        }
    }

    public static Episode.EpisodeBuilder builder() {
        return new Episode.EpisodeBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public TvShow getTvShow() {
        return this.tvShow;
    }

    public Integer getSeasonNumber() {
        return this.seasonNumber;
    }

    public Integer getEpisodeNumber() {
        return this.episodeNumber;
    }

    public String getTitle() {
        return this.title;
    }

    public String getOverview() {
        return this.overview;
    }

    public Integer getRuntime() {
        return this.runtime;
    }

    public Integer getAirYear() {
        return this.airYear;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTvShow(final TvShow tvShow) {
        this.tvShow = tvShow;
    }

    public void setSeasonNumber(final Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public void setEpisodeNumber(final Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setOverview(final String overview) {
        this.overview = overview;
    }

    public void setRuntime(final Integer runtime) {
        this.runtime = runtime;
    }

    public void setAirYear(final Integer airYear) {
        this.airYear = airYear;
    }

    public Episode() {
    }

    public Episode(final Long id, final TvShow tvShow, final Integer seasonNumber, final Integer episodeNumber, final String title, final String overview, final Integer runtime, final Integer airYear) {
        this.id = id;
        this.tvShow = tvShow;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.overview = overview;
        this.runtime = runtime;
        this.airYear = airYear;
    }
    // equals/hashCode omitted for brevity - same pattern as before
}
