package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.shared.Genre;
import com.graphqlguy.moviedb.shared.SearchResult;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tv_shows")
public class TvShow implements SearchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;
    private Double rating;
    private String posterUrl;
    private Integer tmdbId;
    @Column(nullable = false)
    private Integer startYear;
    private Integer endYear; // null if still airing
    private Integer seasons;
    @Column(columnDefinition = "TEXT")
    private String plot;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tvshow_creators", joinColumns = @JoinColumn(name = "tvshow_id"), inverseJoinColumns = @JoinColumn(name = "person_id"))
    private Set<Person> creators = new HashSet<>();
    @OneToMany(mappedBy = "tvShow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TvShowCast> cast = new ArrayList<>();
    @OneToMany(mappedBy = "tvShow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("seasonNumber ASC, episodeNumber ASC")
    private List<Episode> episodes = new ArrayList<>();

    // Same id-based equals/hashCode as every entity since Class 2;
    // Class 8 explains why @BatchMapping's entity-keyed maps depend on it
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TvShow tvShow = (TvShow) o;
        return id != null && id.equals(tvShow.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class TvShowBuilder {
        private Long id;
        private String title;
        private Genre genre;
        private Double rating;
        private String posterUrl;
        private Integer tmdbId;
        private Integer startYear;
        private Integer endYear;
        private Integer seasons;
        private String plot;
        private Set<Person> creators = new HashSet<>();
        private List<TvShowCast> cast = new ArrayList<>();
        private List<Episode> episodes = new ArrayList<>();

        TvShowBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder genre(final Genre genre) {
            this.genre = genre;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder rating(final Double rating) {
            this.rating = rating;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder posterUrl(final String posterUrl) {
            this.posterUrl = posterUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder tmdbId(final Integer tmdbId) {
            this.tmdbId = tmdbId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder startYear(final Integer startYear) {
            this.startYear = startYear;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder endYear(final Integer endYear) {
            this.endYear = endYear;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder seasons(final Integer seasons) {
            this.seasons = seasons;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder plot(final String plot) {
            this.plot = plot;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder creators(final Set<Person> creators) {
            this.creators = creators;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder cast(final List<TvShowCast> cast) {
            this.cast = cast;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShow.TvShowBuilder episodes(final List<Episode> episodes) {
            this.episodes = episodes;
            return this;
        }

        public TvShow build() {
            return new TvShow(this.id, this.title, this.genre, this.rating, this.posterUrl, this.tmdbId, this.startYear, this.endYear, this.seasons, this.plot, this.creators, this.cast, this.episodes);
        }

        @Override
        public String toString() {
            return "TvShow.TvShowBuilder(id=" + this.id + ", title=" + this.title + ", genre=" + this.genre + ", rating=" + this.rating + ", posterUrl=" + this.posterUrl + ", tmdbId=" + this.tmdbId + ", startYear=" + this.startYear + ", endYear=" + this.endYear + ", seasons=" + this.seasons + ", plot=" + this.plot + ", creators=" + this.creators + ", cast=" + this.cast + ", episodes=" + this.episodes + ")";
        }
    }

    public static TvShow.TvShowBuilder builder() {
        return new TvShow.TvShowBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public Genre getGenre() {
        return this.genre;
    }

    public Double getRating() {
        return this.rating;
    }

    public String getPosterUrl() {
        return this.posterUrl;
    }

    public Integer getTmdbId() {
        return this.tmdbId;
    }

    public Integer getStartYear() {
        return this.startYear;
    }

    public Integer getEndYear() {
        return this.endYear;
    }

    public Integer getSeasons() {
        return this.seasons;
    }

    public String getPlot() {
        return this.plot;
    }

    public Set<Person> getCreators() {
        return this.creators;
    }

    public List<TvShowCast> getCast() {
        return this.cast;
    }

    public List<Episode> getEpisodes() {
        return this.episodes;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setGenre(final Genre genre) {
        this.genre = genre;
    }

    public void setRating(final Double rating) {
        this.rating = rating;
    }

    public void setPosterUrl(final String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setTmdbId(final Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public void setStartYear(final Integer startYear) {
        this.startYear = startYear;
    }

    public void setEndYear(final Integer endYear) {
        this.endYear = endYear;
    }

    public void setSeasons(final Integer seasons) {
        this.seasons = seasons;
    }

    public void setPlot(final String plot) {
        this.plot = plot;
    }

    public void setCreators(final Set<Person> creators) {
        this.creators = creators;
    }

    public void setCast(final List<TvShowCast> cast) {
        this.cast = cast;
    }

    public void setEpisodes(final List<Episode> episodes) {
        this.episodes = episodes;
    }

    public TvShow() {
    }

    public TvShow(final Long id, final String title, final Genre genre, final Double rating, final String posterUrl, final Integer tmdbId, final Integer startYear, final Integer endYear, final Integer seasons, final String plot, final Set<Person> creators, final List<TvShowCast> cast, final List<Episode> episodes) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.rating = rating;
        this.posterUrl = posterUrl;
        this.tmdbId = tmdbId;
        this.startYear = startYear;
        this.endYear = endYear;
        this.seasons = seasons;
        this.plot = plot;
        this.creators = creators;
        this.cast = cast;
        this.episodes = episodes;
    }
}
