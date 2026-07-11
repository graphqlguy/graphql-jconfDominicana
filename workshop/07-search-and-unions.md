# Class 7: Search and Unions

Users need to find things by name, and a real search box does not care whether the match is a movie or a person: it returns whatever is relevant, of whatever type. This class builds that, and in doing so introduces GraphQL **union types**, which let a single field return a value that is one of several distinct types.

We approach it in two steps: first two ordinary, single-type searches, then the union that combines them.

By the end of this class, you will:

- Add typed search queries backed by the existing services
- Define a union type and resolve it
- Select per-type fields with inline fragments, and know when a union is the right tool

## 1. Typed search

The service layer already knows how to search: `MovieService.searchByTitle` and `PersonService.searchByName` both exist. We only expose them. Add two queries:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    searchMovies(title: String!): [Movie]
    searchPeople(name: String!): [Person!]!
}
```

The resolvers are as thin as any we have written.

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
@QueryMapping
List<Movie> searchMovies(@Argument String title) {
    return movieService.searchByTitle(title);
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@QueryMapping
List<Person> searchPeople(@Argument String name) {
    return personService.searchByName(name);
}
```

Restart and try them:

```graphql
query {
  searchMovies(title: "godfather") { title }
  searchPeople(name: "eastwood") { name }
}
```

Each returns a list of a single, known type. That is the limitation we address next.

## 2. A union: one search, mixed results

A single search box should query everything at once and return a mixed list. But GraphQL is strongly typed: a field must declare what it returns, and `searchMovies` returns `[Movie]`, nothing else. A **union type** is the tool for "a value that is one of these types." We define one whose members are `Movie` and `Person`:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    search(query: String!): [SearchResult!]!
}

"""Union of Movie and Person for cross-type search"""
union SearchResult = Movie | Person
```

Two things a union requires, on the Java side:

- Its member types need a common Java supertype so a resolver can return a single list of them. The starter provides a marker interface, `SearchResult`, in the `shared` package. `Movie` already implements it; add it to `Person`:

  `src/main/java/com/graphqlguy/moviedb/person/Person.java`

  ```java
  import com.graphqlguy.moviedb.shared.SearchResult;

  public class Person implements SearchResult { ... }
  ```

- The query itself belongs to neither the movie nor the person domain, so it gets its own controller:

  `src/main/java/com/graphqlguy/moviedb/search/SearchController.java`

  ```java
  package com.graphqlguy.moviedb.search;

  import com.graphqlguy.moviedb.movie.MovieService;
  import com.graphqlguy.moviedb.person.PersonService;
  import com.graphqlguy.moviedb.shared.SearchResult;
  import lombok.RequiredArgsConstructor;
  import org.springframework.graphql.data.method.annotation.Argument;
  import org.springframework.graphql.data.method.annotation.QueryMapping;
  import org.springframework.stereotype.Controller;

  import java.util.ArrayList;
  import java.util.List;

  @Controller
  @RequiredArgsConstructor
  public class SearchController {

      private final MovieService movieService;
      private final PersonService personService;

      @QueryMapping
      List<SearchResult> search(@Argument String query) {
          List<SearchResult> results = new ArrayList<>();
          results.addAll(movieService.searchByTitle(query));
          results.addAll(personService.searchByName(query));
          return results;
      }
  }
  ```

The resolver returns a `List<SearchResult>` holding a mix of `Movie` and `Person` objects. How does GraphQL know which is which? By the Java class name: Spring for GraphQL maps a `Movie` instance to the `Movie` type and a `Person` instance to the `Person` type, because the class names match the schema type names. No manual type resolver is needed.

## 3. Querying a union: inline fragments

A union has no fields of its own; a `SearchResult` is *either* a `Movie` *or* a `Person`, and those share nothing to select in common. The client therefore says, for each possible type, which fields to read. This is an **inline fragment**, `... on Type`:

```graphql
query {
  search(query: "godfather") {
    __typename
    ... on Movie { title releaseYear }
    ... on Person { name }
  }
}
```

`__typename` is a built-in field, available on any type, that returns the concrete type name as a string. Searching "godfather" returns three `Movie` results; searching "nolan" returns a `Person`. The client reads `__typename` to decide how to render each entry, which is exactly what the frontend does: the search box now returns movies and people together, in one list, from one query.

## 4. Union or interface?

`Movie | Person` is a good use of a union precisely because the two types are so different. They share no meaningful fields, so forcing the client to spell out `... on Movie` and `... on Person` is honest: there is genuinely nothing to read without knowing the type.

The picture changes when the members are nearly identical. A movie and a television show share a title, a genre, a rating, a poster. If we joined them in a union, every client would have to write two almost-identical inline fragments to read the same fields twice. That is the signal for the other abstraction, an **interface**: a set of fields that multiple types promise to provide, queryable directly without a fragment. We will reach for an interface when we add television shows, and the contrast with this union is the whole lesson.

## Recap

- A union type lets one field return a value that is one of several member types.
- Member types share a Java marker interface so a resolver can return them as one list; Spring resolves the concrete type by matching the class name to the schema type.
- Clients read a union with inline fragments (`... on Type`) and the built-in `__typename`.
- Use a union when the members are genuinely different; use an interface when they share a common set of fields.
