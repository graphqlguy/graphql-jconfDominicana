package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.shared.Content;
import com.graphqlguy.moviedb.shared.Genre;
import com.graphqlguy.moviedb.shared.SearchResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "movies_directors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "director_id")
    )
    private List<Person> directors =  new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY, cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<MovieCast> cast =  new ArrayList<>();

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
}
