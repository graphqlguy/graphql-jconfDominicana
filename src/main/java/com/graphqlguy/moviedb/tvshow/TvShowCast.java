package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.person.Person;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tvshow_cast")
public class TvShowCast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tvshow_id", nullable = false)
    private TvShow tvShow;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;
    private String characterName;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TvShowCast other = (TvShowCast) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    public static class TvShowCastBuilder {
        private Long id;
        private TvShow tvShow;
        private Person person;
        private String characterName;

        TvShowCastBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public TvShowCast.TvShowCastBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShowCast.TvShowCastBuilder tvShow(final TvShow tvShow) {
            this.tvShow = tvShow;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShowCast.TvShowCastBuilder person(final Person person) {
            this.person = person;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public TvShowCast.TvShowCastBuilder characterName(final String characterName) {
            this.characterName = characterName;
            return this;
        }

        public TvShowCast build() {
            return new TvShowCast(this.id, this.tvShow, this.person, this.characterName);
        }

        @Override
        public String toString() {
            return "TvShowCast.TvShowCastBuilder(id=" + this.id + ", tvShow=" + this.tvShow + ", person=" + this.person + ", characterName=" + this.characterName + ")";
        }
    }

    public static TvShowCast.TvShowCastBuilder builder() {
        return new TvShowCast.TvShowCastBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public TvShow getTvShow() {
        return this.tvShow;
    }

    public Person getPerson() {
        return this.person;
    }

    public String getCharacterName() {
        return this.characterName;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTvShow(final TvShow tvShow) {
        this.tvShow = tvShow;
    }

    public void setPerson(final Person person) {
        this.person = person;
    }

    public void setCharacterName(final String characterName) {
        this.characterName = characterName;
    }

    public TvShowCast() {
    }

    public TvShowCast(final Long id, final TvShow tvShow, final Person person, final String characterName) {
        this.id = id;
        this.tvShow = tvShow;
        this.person = person;
        this.characterName = characterName;
    }
}
