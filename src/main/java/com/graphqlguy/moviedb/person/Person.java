package com.graphqlguy.moviedb.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer birthYear;
    @Column(length = 2)
    private String countryCode;
    @Column(length = 1000)
    private String biography;
    private String photoUrl;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person other = (Person) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    public static class PersonBuilder {
        private Long id;
        private String name;
        private Integer birthYear;
        private String countryCode;
        private String biography;
        private String photoUrl;

        PersonBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public Person.PersonBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Person.PersonBuilder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Person.PersonBuilder birthYear(final Integer birthYear) {
            this.birthYear = birthYear;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Person.PersonBuilder countryCode(final String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Person.PersonBuilder biography(final String biography) {
            this.biography = biography;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public Person.PersonBuilder photoUrl(final String photoUrl) {
            this.photoUrl = photoUrl;
            return this;
        }

        public Person build() {
            return new Person(this.id, this.name, this.birthYear, this.countryCode, this.biography, this.photoUrl);
        }

        @Override
        public String toString() {
            return "Person.PersonBuilder(id=" + this.id + ", name=" + this.name + ", birthYear=" + this.birthYear + ", countryCode=" + this.countryCode + ", biography=" + this.biography + ", photoUrl=" + this.photoUrl + ")";
        }
    }

    public static Person.PersonBuilder builder() {
        return new Person.PersonBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Integer getBirthYear() {
        return this.birthYear;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public String getBiography() {
        return this.biography;
    }

    public String getPhotoUrl() {
        return this.photoUrl;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setBirthYear(final Integer birthYear) {
        this.birthYear = birthYear;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public void setBiography(final String biography) {
        this.biography = biography;
    }

    public void setPhotoUrl(final String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Person() {
    }

    public Person(final Long id, final String name, final Integer birthYear, final String countryCode, final String biography, final String photoUrl) {
        this.id = id;
        this.name = name;
        this.birthYear = birthYear;
        this.countryCode = countryCode;
        this.biography = biography;
        this.photoUrl = photoUrl;
    }
}
