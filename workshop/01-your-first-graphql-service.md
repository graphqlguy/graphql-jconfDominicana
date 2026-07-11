# Class 1: Your First GraphQL Service

GraphQL lets clients ask for exactly what they need, and nothing more. In this class we turn our plain Spring Boot application into a GraphQL service: we add the dependency, write our first schema, wire it to the service layer that is already there, and finish with a query that supports filtering, sorting, and pagination.

By the end of this class, you will:

- Have a running GraphQL endpoint with an interactive playground (GraphiQL)
- Understand schema-first development: the schema is the contract, Java implements it
- Write queries with arguments, default values, and input types
- Know how to evolve an API without breaking clients, using `@deprecated`

## 1. Add the dependency

Spring for GraphQL wraps GraphQL Java with familiar Spring conventions, so you rarely need to touch the engine directly. One starter brings in everything we need.

`pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

## 2. Turn on GraphiQL

GraphiQL is an in-browser IDE for GraphQL: query editor, autocomplete, and documentation, all served by our own application. It is disabled by default, so switch it on:

`src/main/resources/application.yaml`

```yaml
spring:
  graphql:
    graphiql:
      enabled: true
```

## 3. Write your first schema

This is the heart of schema-first development: we describe the API in the GraphQL Schema Definition Language (SDL), and only then implement it. Spring picks up any `.graphqls` file under `src/main/resources/graphql/`.

Create the file with one query and one type:

`src/main/resources/graphql/schema.graphqls`

```graphql
# The single entry point of our API. This line is a comment, by the way:
# comments start with # and are only for people reading this file.
type Query {
    moviesAll: [Movie!]!
}

"""
    Custom Movie type
    All movie details are taken from the The Movie Database (TMDB)
    Triple quotes allow multiline documentation.
"""
# We will expand the Movie type as we progress through the tutorial
type Movie {
    id: ID!
    title: String!
    releaseYear: Int!
    genre: Genre
    "Average rating from 0 to 10"
    rating: Float
    "Runtime in minutes"
    runtime: Int
    plot: String
    posterUrl: String
    "The Movie Database (TMDB) external identifier"
    tmdbId: Int
}

enum Genre {
    ACTION
    COMEDY
    CRIME
    DRAMA
    FANTASY
    HORROR
    MYSTERY
    ROMANCE
    SCIFI
    THRILLER
    WAR
    WESTERN
}
```

A few things to notice:

| Syntax | Meaning |
|---|---|
| `type Query` | The entry point. Every field here is a query clients can call. Every GraphQL schema **must** have a `Query` type with at least one field; everything else (mutations, subscriptions) is optional. |
| `[Movie!]!` | A non-null list of non-null movies: the list is always present, and never contains `null` entries. |
| `ID!` | An opaque identifier scalar. The `!` means the field can never be `null`. |
| `enum Genre` | A closed set of values, validated by the engine before your code ever runs. |

### Descriptions vs. comments

SDL has two ways to write text, and they serve completely different audiences:

| Syntax | Name | Who sees it |
|---|---|---|
| `"..."` | Description (single line) | **Clients.** Part of the schema, returned by introspection, shown in GraphiQL and every GraphQL tool. |
| `"""..."""` | Description (multiline) | **Clients.** Same as above, just for longer text spanning several lines. |
| `# ...` | Comment | **Nobody but you.** Stripped when the schema is parsed; it never reaches introspection or GraphiQL. |

Rule of thumb: if a client of your API would benefit from reading it, make it a description. If it is a note to whoever edits the schema file, make it a comment.

## 4. Start the server and explore

Start the application:

```bash
./mvnw spring-boot:run
```

> [!NOTE]
> If startup fails with `Port 8080 was already in use`, another process is holding the port. Change it in two places, because the frontend forwards its API calls to the backend:
>
> 1. **Backend**, in `src/main/resources/application.yaml`:
>    ```yaml
>    server:
>      port: 8090
>    ```
> 2. **Frontend**, in `frontend/vite.config.js`, so the proxy points at the same port:
>    ```js
>    '/graphql': {
>      target: 'http://localhost:8090',
>      ws: true,
>    }
>    ```
>
> Use your chosen port in place of `8080` in the URLs below.

Open GraphiQL at [http://localhost:8080/graphiql](http://localhost:8080/graphiql) and click the book icon (top left) to open the documentation explorer. Our `moviesAll` query and the `Movie` type are already there. Nobody documented this by hand: clients discover the API by asking the schema itself. This is called **introspection**, and we will meet it again.

While you are in the docs, check our two kinds of text: the descriptions on `rating`, `runtime`, and `tmdbId` are all there (single-quoted and triple-quoted alike), but the `#` comment above `type Query` is nowhere to be found. Comments never leave the schema file.

Now run the query:

```graphql
query {
  moviesAll {
    id
    title
  }
}
```

You get an error. That is expected: the schema declares *what* exists, but nothing on the Java side answers the call yet. The contract is public; the implementation is missing.

## 5. Implement the controller

The service layer already exists (`MovieService` with a database full of seeded movies), so all we add is the GraphQL entry point. Spring for GraphQL maps schema fields to annotated controller methods, so we need a controller.

**Create a new Java class** named `MovieController` in the `com.graphqlguy.moviedb.movie` package, that is, next to `MovieService` in `src/main/java/com/graphqlguy/moviedb/movie/`. This is the first class we write in the workshop; every later controller follows the same shape.

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
package com.graphqlguy.moviedb.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @QueryMapping
    List<Movie> moviesAll() {
        return movieService.findAll();
    }
}
```

| Annotation | Meaning |
|---|---|
| `@Controller` | A regular Spring bean; Spring for GraphQL scans it for handler methods. |
| `@QueryMapping` | Binds this method to the `Query` field with the same name (`moviesAll`). |
| `@RequiredArgsConstructor` | Lombok generates a constructor for all `final` fields, and Spring injects `MovieService` through it. |

> [!NOTE]
> The controller lives in the `movie` package on purpose: some `MovieService` methods are package-private, which keeps the service's API deliberately narrow. The project is organized by domain packages, not by technical layers.

> [!TIP]
> If you are on the Lombok-free `main_no_lombok` branch, replace the Lombok annotation with a hand-written constructor that assigns the service field.

Restart the application and run the query again. The response now contains all 52 seeded movies. Remove fields from the query, add others, and observe how the response always mirrors your selection exactly. That is GraphQL's core promise: **the client controls the shape of the response**.

## 6. Fetch a single movie

Lists are nice, but clients also need details for one movie. This time we add an argument. Schema first:

> [!NOTE]
> From here on, schema snippets show only **what changes**. A `# ...existing queries...` comment marks content that is already in the file and stays as it is. Add the new lines to the existing `Query` type; do not replace the whole block.

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    movie(id: ID!): Movie
}
```

Note the return type: `Movie`, not `Movie!`. If the id does not exist, the field is allowed to be `null` instead of failing the entire response.

Then Java:

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
import org.springframework.graphql.data.method.annotation.Argument;

@QueryMapping
Movie movie(@Argument Long id) {
    return movieService.findById(id);
}
```

`@Argument` binds the GraphQL argument to the method parameter by name, converting the `ID` scalar to a `Long` on the way. Restart and try it:

```graphql
query {
  movie(id: 29) {
    title
    plot
    runtime
  }
}
```

## 7. Evolve the API: filtering, sorting, pagination

`moviesAll` returns the entire dataset in a single response. That works with 52 seeded movies, but it does not scale: with a real catalog, response times and memory usage grow without bound. We need pagination and filtering. But clients may already depend on `moviesAll`, so we can't just remove it.

First we mark the old query as deprecated, then we add the better one. Schema first, and this step has two parts.

Change the existing `moviesAll` line to add the `@deprecated` directive:

`src/main/resources/graphql/schema.graphqls`

```graphql
    moviesAll: [Movie!]! @deprecated(reason: "Unbounded. Use 'movies' instead.")
```

Add the new `movies` query to the `Query` type:

```graphql
type Query {
    # ...existing queries...
    movies(filter: MovieFilter, page: Int = 0, size: Int = 10, sort: MovieSort): MoviePage!
}
```

And add the supporting types below (these are new types, not part of `Query`):

```graphql
enum MovieSortField {
    TITLE
    RELEASE_YEAR
    RATING
    RUNTIME
}

enum SortOrder {
    ASC
    DESC
}

input MovieFilter {
    genre: Genre
    minRating: Float
    maxRating: Float
    minYear: Int
    maxYear: Int
    titleContains: String
}

input MovieSort {
    field: MovieSortField
    order: SortOrder
}

type MoviePage {
    content: [Movie!]!
    totalElements: Int!
    totalPages: Int!
    currentPage: Int!
    size: Int!
    isFirst: Boolean!
    isLast: Boolean!
    hasNext: Boolean!
    hasPrevious: Boolean!
}
```

Two new pieces of SDL here:

- **`input` types** are arguments-only shapes. An `input` can never be returned, and a `type` can never be an argument; GraphQL keeps the two directions strictly apart.
- **`@deprecated`** keeps the field working but marks it as deprecated in every tool. Open GraphiQL's docs and look at `moviesAll` now: it is struck through, with our reason attached. Existing clients keep working, and every consumer of the schema can see what to migrate to.

The Java side is one method. The matching `MovieFilter`, `MovieSort`, and `MoviePage` records and the service logic already exist in the starter (peek at `MovieService.findMovies` if you are curious how the filter reaches the database):

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
@QueryMapping
MoviePage movies(@Argument MovieFilter filter, @Argument Integer page,
                 @Argument Integer size, @Argument MovieSort sort) {
    return movieService.findMovies(filter, page != null ? page : 0,
            size != null ? size : 10, sort);
}
```

Restart the application and try the new query:

```graphql
query {
  movies(
    filter: { genre: SCIFI, minRating: 8.0 }
    sort: { field: RATING, order: DESC }
    page: 0
    size: 5
  ) {
    totalElements
    content {
      title
      rating
    }
  }
}
```

## 8. Start the frontend

Let's see our API through the eyes of a real client. The repository ships a React frontend that introspects our schema at startup and enables exactly the features that exist. The first time (and only the first time), install the dependencies:

```bash
cd frontend
npm install    # first time only
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The movie grid is live: posters, genre filters, rating filter, sorting, pagination, all powered by the `movies` query you just wrote. Everything else in the app is still waiting for its part of the schema.

> [!TIP]
> Whenever you extend the schema from now on: restart the backend, refresh the browser. The frontend re-introspects on every reload and lights up whatever is new.

## Exercises

The frontend has a People section waiting for two queries. The service layer is already there (`PersonService`), as are the `Person` entity and the `PersonPage` record. You write the schema and the controller, following the same two steps as above: schema first, then Java.

### Exercise 1: list people

Add a paginated `people` query returning a `PersonPage` with `content`, `totalElements`, `totalPages`, `currentPage`, and `size`. A `Person` has `id`, `name`, `birthYear`, `biography`, and `photoUrl`; decide for yourself which fields can be null. Use `PersonService.findAll(page, size)`. When it works, the People link appears in the frontend's navigation.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    people(page: Int = 0, size: Int = 20): PersonPage!
}

type Person {
    id: ID!
    name: String!
    birthYear: Int
    """Short biography blurb"""
    biography: String
    """Portrait image URL (TMDB)"""
    photoUrl: String
}

"""A page of people"""
type PersonPage {
    content: [Person!]!
    totalElements: Int!
    totalPages: Int!
    currentPage: Int!
    size: Int!
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
package com.graphqlguy.moviedb.person;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @QueryMapping
    PersonPage people(@Argument Integer page, @Argument Integer size) {
        return personService.findAll(page != null ? page : 0, size != null ? size : 20);
    }
}
```

</details>

### Exercise 2: fetch a single person

Add a `person(id: ID!)` query. `PersonService.findById` returns the person, or throws `EntityNotFoundException` when the id does not exist. Query a non-existent id in GraphiQL and observe what GraphQL does with an exception (we will make that error response much nicer in a later class). With this query in place, clicking a person in the frontend opens their profile page.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    person(id: ID!): Person
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@QueryMapping
Person person(@Argument Long id) {
    return personService.findById(id);
}
```

</details>

## Recap

- One dependency and one config line gave us a GraphQL endpoint and an interactive playground.
- The schema is the contract: we wrote SDL first, every time, and implemented it second.
- `@QueryMapping` binds `Query` fields to controller methods; `@Argument` binds arguments.
- Clients pick their fields; the server never over-sends.
- APIs evolve in place: `@deprecated` plus a better query, no `/v2`.

Next up: movies have directors, and directors have movies. Relationships are where GraphQL starts to shine.
