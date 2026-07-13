package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.shared.Genre;
import com.graphqlguy.moviedb.shared.Content;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tv_shows")
public class TvShow implements SearchResult, Content {

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

    private Integer endYear;       // null if still airing
    private Integer seasons;

    @Column(columnDefinition = "TEXT")
    private String plot;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tvshow_creators",
            joinColumns = @JoinColumn(name = "tvshow_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    @Builder.Default
    private Set<Person> creators = new HashSet<>();

    @OneToMany(mappedBy = "tvShow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TvShowCast> cast = new ArrayList<>();

    @OneToMany(mappedBy = "tvShow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("seasonNumber ASC, episodeNumber ASC")
    @Builder.Default
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
}