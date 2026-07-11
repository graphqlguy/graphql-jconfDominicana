package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.person.Person;
import jakarta.persistence.*;

@Entity
public class MovieCast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String characterName;
    @JoinColumn(name = "movie_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Movie movie;
    @JoinColumn(name = "person_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Person person;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieCast other = (MovieCast) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    public static class MovieCastBuilder {
        private Long id;
        private String characterName;
        private Movie movie;
        private Person person;

        MovieCastBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public MovieCast.MovieCastBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MovieCast.MovieCastBuilder characterName(final String characterName) {
            this.characterName = characterName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MovieCast.MovieCastBuilder movie(final Movie movie) {
            this.movie = movie;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MovieCast.MovieCastBuilder person(final Person person) {
            this.person = person;
            return this;
        }

        public MovieCast build() {
            return new MovieCast(this.id, this.characterName, this.movie, this.person);
        }

        @Override
        public String toString() {
            return "MovieCast.MovieCastBuilder(id=" + this.id + ", characterName=" + this.characterName + ", movie=" + this.movie + ", person=" + this.person + ")";
        }
    }

    public static MovieCast.MovieCastBuilder builder() {
        return new MovieCast.MovieCastBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getCharacterName() {
        return this.characterName;
    }

    public Movie getMovie() {
        return this.movie;
    }

    public Person getPerson() {
        return this.person;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setCharacterName(final String characterName) {
        this.characterName = characterName;
    }

    public void setMovie(final Movie movie) {
        this.movie = movie;
    }

    public void setPerson(final Person person) {
        this.person = person;
    }

    public MovieCast() {
    }

    public MovieCast(final Long id, final String characterName, final Movie movie, final Person person) {
        this.id = id;
        this.characterName = characterName;
        this.movie = movie;
        this.person = person;
    }
}
