package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.shared.Content;
import com.graphqlguy.moviedb.shared.Genre;
import com.graphqlguy.moviedb.shared.SearchResult;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Movie implements SearchResult, Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 200)
    private String title;
    private Integer releaseYear;
    @Enumerated(EnumType.STRING)
    private Genre genre;
    private Double rating;
    private Integer runtime;
    @Column(length = 2000)
    private String plot;
    @Column(length = 500)
    private String posterUrl;
    private Integer tmdbId;
    @ManyToMany
    @JoinTable(name = "movies_directors", joinColumns = @JoinColumn(name = "movie_id"), inverseJoinColumns = @JoinColumn(name = "director_id"))
    private List<Person> directors = new ArrayList<>();
    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieCast> cast = new ArrayList<>();

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie other = (Movie) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class MovieBuilder {
        private Long id;
        private String title;
        private Integer releaseYear;
        private Genre genre;
        private Double rating;
        private Integer runtime;
        private String plot;
        private String posterUrl;
        private Integer tmdbId;
        private List<Person> directors = new ArrayList<>();
        private List<MovieCast> cast = new ArrayList<>();

        MovieBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder releaseYear(final Integer releaseYear) {
            this.releaseYear = releaseYear;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder genre(final Genre genre) {
            this.genre = genre;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder rating(final Double rating) {
            this.rating = rating;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder runtime(final Integer runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder plot(final String plot) {
            this.plot = plot;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder posterUrl(final String posterUrl) {
            this.posterUrl = posterUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder tmdbId(final Integer tmdbId) {
            this.tmdbId = tmdbId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder directors(final List<Person> directors) {
            this.directors = directors;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Movie.MovieBuilder cast(final List<MovieCast> cast) {
            this.cast = cast;
            return this;
        }

        public Movie build() {
            return new Movie(this.id, this.title, this.releaseYear, this.genre, this.rating, this.runtime, this.plot, this.posterUrl, this.tmdbId, this.directors, this.cast);
        }

        @Override
        public String toString() {
            return "Movie.MovieBuilder(id=" + this.id + ", title=" + this.title + ", releaseYear=" + this.releaseYear + ", genre=" + this.genre + ", rating=" + this.rating + ", runtime=" + this.runtime + ", plot=" + this.plot + ", posterUrl=" + this.posterUrl + ", tmdbId=" + this.tmdbId + ", directors=" + this.directors + ", cast=" + this.cast + ")";
        }
    }

    public static Movie.MovieBuilder builder() {
        return new Movie.MovieBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public Integer getReleaseYear() {
        return this.releaseYear;
    }

    public Genre getGenre() {
        return this.genre;
    }

    public Double getRating() {
        return this.rating;
    }

    public Integer getRuntime() {
        return this.runtime;
    }

    public String getPlot() {
        return this.plot;
    }

    public String getPosterUrl() {
        return this.posterUrl;
    }

    public Integer getTmdbId() {
        return this.tmdbId;
    }

    public List<Person> getDirectors() {
        return this.directors;
    }

    public List<MovieCast> getCast() {
        return this.cast;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setReleaseYear(final Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public void setGenre(final Genre genre) {
        this.genre = genre;
    }

    public void setRating(final Double rating) {
        this.rating = rating;
    }

    public void setRuntime(final Integer runtime) {
        this.runtime = runtime;
    }

    public void setPlot(final String plot) {
        this.plot = plot;
    }

    public void setPosterUrl(final String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setTmdbId(final Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public void setDirectors(final List<Person> directors) {
        this.directors = directors;
    }

    public void setCast(final List<MovieCast> cast) {
        this.cast = cast;
    }

    public Movie() {
    }

    public Movie(final Long id, final String title, final Integer releaseYear, final Genre genre, final Double rating, final Integer runtime, final String plot, final String posterUrl, final Integer tmdbId, final List<Person> directors, final List<MovieCast> cast) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.rating = rating;
        this.runtime = runtime;
        this.plot = plot;
        this.posterUrl = posterUrl;
        this.tmdbId = tmdbId;
        this.directors = directors;
        this.cast = cast;
    }
}
