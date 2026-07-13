# Class 9: TV Shows, Interfaces, and a Watch List

We have put television shows off until now on purpose. On their own they teach little: a `TvShow` is resolved exactly like a `Movie`, and you have written that code several times. What TV shows give us is a second type that is genuinely *like* a movie, and that unlocks two parts of the type system we have not met yet:

- an **interface**, for the fields a movie and a show have in common, and
- a **`@oneOf` input**, for an argument that is exactly one of several things.

We reach both through one small feature that needs them: a personal **watch list**, a mixed list of movies and shows a user wants to watch or has watched.

By the end of this class, you will:

- Expose TV shows through the schema (quickly, since it mirrors movies)
- Define an interface and understand when it beats a union
- Accept a "one of these" argument with a `@oneOf` input type
- Build a per-user, authenticated feature on top of both

As always, the domain layer is already in the starter: the `TvShow`, `Episode`, and `WatchlistItem` entities, their repositories, and the `TvShowService` and `WatchlistService` are scaffolding, the same as `Movie` and `Review`. This class writes the GraphQL.

## 1. TV shows (the quick part)

There is nothing new here. It is the movie pattern applied to a second type: a couple of queries and a controller whose field resolvers are batched to avoid N+1, exactly as in Class 2.

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    tvShow(id: ID!): TvShow
    tvShows(page: Int = 0 @Range(min: 0), size: Int = 10 @Range(min: 1, max: 100)): TvShowPage!
}

"""A television show"""
type TvShow {
    id: ID!
    title: String!
    genre: Genre
    "Average rating on a scale of 0-10"
    rating: Float
    posterUrl: String
    startYear: Int!
    "Null while the show is still airing"
    endYear: Int
    seasons: Int
    plot: String
    creators: [Person!]!
    cast: [TvShowCast!]!
    episodes: [Episode!]!
}

"""A single episode of a TV show"""
type Episode {
    id: ID!
    seasonNumber: Int!
    episodeNumber: Int!
    title: String!
    overview: String
    "Runtime in minutes"
    runtime: Int
    airYear: Int
}

"""An actor's role in a TV show, linking a person to a character"""
type TvShowCast {
    id: ID!
    characterName: String!
    person: Person!
    tvShow: TvShow!
}

"""A page of TV shows"""
type TvShowPage {
    content: [TvShow!]!
    totalElements: Int!
    totalPages: Int!
    currentPage: Int!
    size: Int!
}
```

`src/main/java/com/graphqlguy/moviedb/tvshow/TvShowController.java` (new)

```java
package com.graphqlguy.moviedb.tvshow;

import com.graphqlguy.moviedb.person.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TvShowController {

    private final TvShowService tvShowService;

    @QueryMapping
    TvShow tvShow(@Argument Long id) {
        return tvShowService.findById(id);
    }

    @QueryMapping
    TvShowPage tvShows(@Argument Integer page, @Argument Integer size) {
        return tvShowService.findAll(page != null ? page : 0, size != null ? size : 10);
    }

    @BatchMapping(typeName = "TvShow")
    Map<TvShow, Set<Person>> creators(List<TvShow> shows) {
        Set<Long> ids = shows.stream().map(TvShow::getId).collect(Collectors.toSet());
        Map<Long, Set<Person>> byShowId = tvShowService.findCreatorsByShowIds(ids);
        Map<TvShow, Set<Person>> result = new HashMap<>();
        for (TvShow show : shows) {
            result.put(show, byShowId.getOrDefault(show.getId(), Set.of()));
        }
        return result;
    }

    @BatchMapping(typeName = "TvShow")
    Map<TvShow, List<TvShowCast>> cast(List<TvShow> shows) {
        Set<Long> ids = shows.stream().map(TvShow::getId).collect(Collectors.toSet());
        Map<Long, List<TvShowCast>> byShowId = tvShowService.findCastByShowIds(ids);
        Map<TvShow, List<TvShowCast>> result = new HashMap<>();
        for (TvShow show : shows) {
            result.put(show, byShowId.getOrDefault(show.getId(), List.of()));
        }
        return result;
    }

    @BatchMapping(typeName = "TvShow")
    Map<TvShow, List<Episode>> episodes(List<TvShow> shows) {
        Set<Long> ids = shows.stream().map(TvShow::getId).collect(Collectors.toSet());
        Map<Long, List<Episode>> byShowId = tvShowService.findEpisodesByShowIds(ids);
        Map<TvShow, List<Episode>> result = new HashMap<>();
        for (TvShow show : shows) {
            result.put(show, byShowId.getOrDefault(show.getId(), List.of()));
        }
        return result;
    }
}
```

Restart, and the frontend's TV Shows section lights up. That is the whole point of doing this quickly: you have seen all of it. Now we make TV shows earn their place.

## 2. An interface: the fields movies and shows share

In Class 7 we combined `Movie` and `Person` in a **union**, and it was the right tool precisely because the two types share nothing to select in common. A movie and a television show are the opposite case. Both have a title, a genre, a rating, a poster. A union here would force every client to write two nearly identical inline fragments to read the same fields twice.

That is the signal for an **interface**: a set of fields that multiple types promise to provide, and that a client can select directly, without a fragment.

`src/main/resources/graphql/schema.graphqls`

```graphql
"""Something watchable: the fields a Movie and a TV show have in common."""
interface Content {
    title: String!
    genre: Genre
    rating: Float
    posterUrl: String
}

type Movie implements Content {
    # ...unchanged...
}

type TvShow implements Content {
    # ...unchanged...
}
```

A type that implements an interface must declare every field the interface lists, which `Movie` and `TvShow` already do. On the Java side, resolution works just as it did for the union: the starter provides a `Content` marker interface in the `shared` package, `Movie` and `TvShow` already implement it, and Spring for GraphQL maps a `Movie` instance to the `Movie` type and a `TvShow` instance to the `TvShow` type by matching the class name. No manual type resolver is needed.

> **A note on nullability.** `Content.genre` is nullable (`Genre`), and both `Movie` and `TvShow` declare `genre: Genre` to match. An implementing type is allowed to be *stricter* than its interface, so a type could legally declare `genre: Genre!` while the interface stays nullable. That is common in real schemas, where types evolve separately and drift apart. It matters for one reason we will see in a moment: if two types declare a shared field with different nullability, a client cannot select that field inside each fragment, because the two response shapes conflict. Reading shared fields off the interface avoids the problem entirely, which is another reason to prefer it.

An interface is only useful once something returns it. That something is the watch list.

## 3. A watch list

A watch list is a personal, mixed collection: some movies, some shows, each marked as something the user wants to watch or has already watched. "A movie or a show" is the interface. "Exactly one of a movie or a show," when you add an entry, is the `@oneOf` input.

### The types

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    "The signed-in user's watch list."
    watchlist: [WatchlistItem!]!
}

type Mutation {
    # ...existing mutations...
    "Add a movie or TV show to the signed-in user's watch list."
    addToWatchlist(subject: WatchlistSubjectInput!, status: WatchStatus): WatchlistItem!
    "Change a watch-list entry's status, e.g. mark it watched."
    setWatchStatus(itemId: ID!, status: WatchStatus!): WatchlistItem!
    "Remove an entry from the watch list."
    removeFromWatchlist(itemId: ID!): Boolean!
}

"""A single entry in a user's watch list: a movie or a show, plus its status."""
type WatchlistItem {
    id: ID!
    content: Content!
    status: WatchStatus!
}

enum WatchStatus {
    WANT_TO_WATCH
    WATCHED
}

"""Identifies the title to add. @oneOf: provide exactly one of movieId or tvShowId."""
input WatchlistSubjectInput @oneOf {
    movieId: ID
    tvShowId: ID
}
```

Two things are worth stopping on.

**`content: Content!`** is the interface in action. A `WatchlistItem` points at either a movie or a show, and the field is typed as the interface, so a single list can hold both.

**`input WatchlistSubjectInput @oneOf`** is a **one-of input type**. The `@oneOf` directive tells the server that exactly one of the fields must be provided: not zero, not both. Its fields must therefore be nullable (`ID`, not `ID!`), because on any given call all but one are absent. The server validates this for you, before your resolver runs. On the Java side it maps to an ordinary record; the guarantee is that exactly one accessor is non-null:

```java
public record WatchlistSubjectInput(String movieId, String tvShowId) {}
```

### The controller

`src/main/java/com/graphqlguy/moviedb/watchlist/WatchlistController.java` (new)

```java
package com.graphqlguy.moviedb.watchlist;

import com.graphqlguy.moviedb.shared.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @QueryMapping
    List<WatchlistItem> watchlist(Principal principal) {
        return watchlistService.findForUser(principal.getName());
    }

    // The interface field: a WatchlistItem holds either a Movie or a TvShow, and
    // both implement Content, so we return whichever is set. Spring picks the
    // GraphQL type from the concrete class name.
    @SchemaMapping(typeName = "WatchlistItem")
    Content content(WatchlistItem item) {
        return item.getMovie() != null ? item.getMovie() : item.getTvShow();
    }

    @MutationMapping
    WatchlistItem addToWatchlist(@Argument WatchlistSubjectInput subject,
                                 @Argument WatchStatus status,
                                 Principal principal) {
        return watchlistService.addToWatchlist(subject, status, principal.getName());
    }

    @MutationMapping
    WatchlistItem setWatchStatus(@Argument Long itemId, @Argument WatchStatus status, Principal principal) {
        return watchlistService.setStatus(itemId, status, principal.getName());
    }

    @MutationMapping
    boolean removeFromWatchlist(@Argument Long itemId, Principal principal) {
        return watchlistService.removeFromWatchlist(itemId, principal.getName());
    }
}
```

The `content` resolver is where the interface pays off. It returns `Content`, and because `Movie` and `TvShow` both implement it, either fits, and Spring resolves the concrete GraphQL type from the class. The `Principal` parameter is the signed-in user, injected by Spring Security from the JWT you built in Class 4; the service uses it to scope the list to that user and to enforce that you can only change your own entries. The `WatchlistService` that does this, along with the entity and repository, is scaffolding, so this controller is all the GraphQL you add.

### Querying it: shared fields off the interface

A watch list is worth nothing to look at unless you sign in. In GraphiQL, add a `Bearer` token (log in through `login` first), then:

```graphql
query {
  watchlist {
    status
    content {
      __typename
      title
      genre
      rating
      posterUrl
      ... on Movie { releaseYear }
      ... on TvShow { seasons }
    }
  }
}
```

Look at how the selection is shaped. `title`, `genre`, `rating`, and `posterUrl` are read **directly on `content`**, once, because they are interface fields; every member has them. Only the type-specific fields, `releaseYear` for a movie and `seasons` for a show, need an inline fragment. That is exactly the economy an interface buys and a union cannot: the common fields are written once, not duplicated per type.

This is also where the nullability note from earlier becomes concrete. If you tried to pull `genre` up into both fragments instead, and the two types declared it with different nullability, the server would reject the query with a fields-conflict error. Reading it off the interface sidesteps that, and reads better regardless.

Adding an entry uses the `@oneOf` input. Provide exactly one key:

```graphql
mutation {
  addToWatchlist(subject: { movieId: 2 }, status: WANT_TO_WATCH) {
    id
    status
    content { __typename title }
  }
}
```

Try to break the `@oneOf` contract, and the server stops you before the resolver runs:

```graphql
mutation {
  addToWatchlist(subject: { movieId: 2, tvShowId: 1 }) { id }
}
```

returns `Exactly one key must be specified for OneOf type 'WatchlistSubjectInput'`. The same happens for an empty `subject: {}`. You never have to write that check yourself.

In the frontend, all of this is already wired: a **Watch List** appears in the navigation once you sign in, movie and show detail pages gain "Want to watch" and "Watched" buttons, and the watch-list page renders the mixed list, reading shared fields off the `Content` interface exactly as above.

## Recap

- TV shows are resolved like any other type; the value in adding them is the pair of constructs they unlock.
- An **interface** declares fields that several types share, selectable directly without a fragment. Prefer it over a union when the members overlap; prefer a union when they do not.
- Concrete-type resolution for an interface works like a union: a shared Java marker plus class-name matching, no manual resolver.
- Read shared fields off the interface and reserve inline fragments for type-specific ones. This is both cleaner and immune to the nullability conflict that duplicated selections can cause.
- A **`@oneOf` input** models an argument that is exactly one of several options. Its fields are nullable, and the server validates the "exactly one" rule for you.
- None of this changed the domain layer: entities, repositories, and services were already there. The class was schema and resolvers.

---

## Exercise: a person's television work

A `Person` already exposes the movies they directed and their movie cast credits. Now that shows exist, give a person their television work too: add `createdShows` and `tvShowCastCredits` to the `Person` type, so a person's page lists the shows they created and the roles they played. The `PersonService` already has the batched lookups; this is the reverse-field pattern from Class 2 applied to two more relationships.

<details>
<summary>Solution</summary>

**Schema** (`Person` type in `schema.graphqls`):

```graphql
type Person {
    # ...existing fields...
    createdShows: [TvShow!]!
    tvShowCastCredits: [TvShowCast!]!
}
```

**Resolvers** (`PersonController.java`):

```java
@BatchMapping
Map<Person, List<TvShow>> createdShows(List<Person> people) {
    List<Long> personIds = people.stream().map(Person::getId).toList();
    Map<Long, List<TvShow>> showsByPersonId = personService.findCreatedShowsByPersonIds(personIds);
    return people.stream()
            .collect(Collectors.toMap(person -> person,
                    person -> showsByPersonId.getOrDefault(person.getId(), List.of())));
}

@BatchMapping
Map<Person, List<TvShowCast>> tvShowCastCredits(List<Person> people) {
    List<Long> personIds = people.stream().map(Person::getId).toList();
    Map<Long, List<TvShowCast>> creditsByPersonId = personService.findTvShowCastCreditsByPersonIds(personIds);
    return people.stream()
            .collect(Collectors.toMap(person -> person,
                    person -> creditsByPersonId.getOrDefault(person.getId(), List.of())));
}
```

Both are `@BatchMapping` resolvers so a page of people triggers one query per relationship, not one per person. Restart, open a director's page, and their shows appear alongside their films.

</details>
</content>
