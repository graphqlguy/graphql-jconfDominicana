# Class 2: Relationships and Nested Resolution

A movie database is not a set of flat tables, it is a graph: movies connect to the people who made them, and people connect back to their work. In this class we expose those relationships in the schema and learn how GraphQL actually resolves nested fields, first automatically, then with our own resolver code.

By the end of this class, you will:

- Expose entity relationships as nested fields in the schema
- Understand default field resolution: how GraphQL finds values without any resolver code
- Write your first `@SchemaMapping` resolver for a field that has no matching Java property
- Reproduce the N+1 problem and understand exactly where it comes from
- Eliminate it with `@BatchMapping`, without changing the schema
- Model a relationship that carries data of its own with a junction type

## 1. Add directors to Movie

Our `Movie` JPA entity already carries the relationship: a `@ManyToMany` list of `Person` directors, mapped through a join table. The schema just does not mention it yet. Add one line to the `Movie` type:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Movie {
    # ...existing fields...
    directors: [Person!]!
}
```

Restart the application and run a nested query in GraphiQL:

```graphql
query {
  movies(size: 3, sort: { field: RATING, order: DESC }) {
    content {
      title
      directors {
        name
        birthYear
      }
    }
  }
}
```

It works, and we wrote no Java at all. Refresh the frontend and the director names appear on the movie cards too.

### Why does this work?

For every field in a query, GraphQL needs a resolver: a piece of code that produces the value. When we do not provide one, the engine falls back to the **default resolver** (`PropertyDataFetcher`), which looks at the parent object and calls the matching getter. Our `movies` query returns `Movie` entities, each `Movie` has a `getDirectors()` method, so `directors` resolves by itself.

This is the general rule behind everything we built so far: `title`, `releaseYear`, and the rest were resolved the same way. A field only needs custom code when the parent object cannot answer for it.

## 2. The reverse direction: directedMovies on Person

Clients will also navigate the other way: from a person to the movies they directed. Add the field to `Person`:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Person {
    # ...existing fields...
    """Movies this person directed"""
    directedMovies: [Movie!]!
}
```

Restart and try it:

```graphql
query {
  person(id: 1) {
    name
    directedMovies {
      title
      releaseYear
    }
  }
}
```

This time we get an error: the field resolved to `null`, and `[Movie!]!` promises it never will. The default resolver returned nothing, because `Person` has no `directedMovies` property. The relationship is owned by the `Movie` side; the `Person` entity knows nothing about it.

This is the moment for our first **schema mapping**: a controller method that resolves one field of one type.

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
import com.graphqlguy.moviedb.movie.Movie;
import org.springframework.graphql.data.method.annotation.SchemaMapping;

@SchemaMapping
List<Movie> directedMovies(Person person) {
    return personService.findDirectedMovies(person);
}
```

| Element | Meaning |
|---|---|
| `@SchemaMapping` | Binds this method to a field, by convention: the method name is the field name (`directedMovies`), and the parameter type is the parent type (`Person`). |
| `Person person` | The parent object, exactly as the previous resolver produced it. GraphQL hands it to us so we can resolve the child field. |

The service side already exists: `PersonService.findDirectedMovies` asks `MovieRepository.findByDirectorsContaining(person)` for every movie whose directors include this person.

Restart, run the query again, and the person's filmography is there. In the frontend, a person's profile page now shows the movies they directed.

Notice the symmetry with class 1: for `Query` fields we used `@QueryMapping`; for fields of any other type we use `@SchemaMapping`. In fact, `@QueryMapping` is just shorthand for `@SchemaMapping` on the `Query` type.

## 3. The hidden cost: N+1

Our resolver works, but think about *when* it runs: once per person. Ask for one person and their movies, and it runs once. Ask for a list of ten people and their movies, and it runs ten times, each time firing its own database query. One query for the list, plus N queries for the nested field: this is the **N+1 problem**, and it is the classic performance trap of every GraphQL API (and APIs in general).

On a laptop with an in-memory database, eleven queries complete in a few milliseconds, so the cost is invisible until we instrument it. The starter has two switches for exactly this. In `application.yaml`, turn on SQL logging and give every service call a realistic latency:

`src/main/resources/application.yaml`

```yaml
spring:
  jpa:
    show-sql: true

demo:
  latency: 1s
```

The latency comes from the starter's `LatencySimulator`: every service method pauses for the configured duration before touching the database, simulating what a real network round trip to a production database costs. At `0s` (the default) it does nothing.

Restart the application, open GraphiQL, and run:

```graphql
query MyQuery {
  people(size: 10) {
    content {
      name
      directedMovies {
        title
      }
    }
  }
}
```

The cost is now clearly visible:

- The response takes around **eleven seconds**: one paused service call for the `people` page, then ten more, one per person, strictly for `directedMovies`.
- The console tells the same story in SQL: one `select` for the people, followed by ten near-identical `select` statements for movies, differing only in the person id.

Nothing is broken. Every resolver did its job correctly; the problem is that nobody looked at the query as a whole. GraphQL resolves field by field, and our `directedMovies` resolver cannot see that its ten invocations could have been a single `where person_id in (...)` query.

Scale the numbers and the cost becomes serious: 100 people per page against a real database at 5 ms per round trip is over half a second of pure latency for a single page, and every additional nested relationship multiplies it.

Keep this query: in the next step we fix it without changing the schema at all, and the before/after comparison is the entire argument for batch loading.

## 4. The fix: batch loading with @BatchMapping

The insight behind the fix: instead of resolving `directedMovies` once per person, collect **all** the people in the current query first, then resolve their movies in a single call. GraphQL calls this pattern a `DataLoader`; Spring for GraphQL exposes it as `@BatchMapping`.

The data access for this is already in the starter, because it is plain repository and grouping code, not GraphQL. The repository loads the movies of many directors in one statement:

`src/main/java/com/graphqlguy/moviedb/movie/MovieRepository.java`

```java
@Query("select distinct m from Movie m left join fetch m.directors where m in " +
        "(select m2 from Movie m2 join m2.directors d where d.id in :personIds)")
List<Movie> findAllWithDirectorsByDirectorIdIn(@Param("personIds") List<Long> personIds);
```

The `in :personIds` clause is the entire point: one statement, however many directors we ask about. The `left join fetch` loads each movie's director list in the same statement, so the grouping does not trigger lazy loading (which would quietly reintroduce N+1 through the back door).

On top of it, `PersonService.findDirectedMoviesByPersonIds` groups the movies by director id and returns a `Map<Long, List<Movie>>`. Read it before continuing; note the single `pause()` (one service call, one simulated round trip, regardless of how many people we resolve), and that a movie with two directors on the requested page lands in both of their lists.

The only thing left to write is the GraphQL side. Replace the `@SchemaMapping` resolver from step 2 with:

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@BatchMapping
Map<Person, List<Movie>> directedMovies(List<Person> people) {
    List<Long> personIds = people.stream().map(Person::getId).toList();
    Map<Long, List<Movie>> moviesByPersonId = personService.findDirectedMoviesByPersonIds(personIds);
    return people.stream()
            .collect(Collectors.toMap(person -> person,
                    person -> moviesByPersonId.getOrDefault(person.getId(), List.of())));
}
```

Compare the signatures, because they tell the whole story:

| Step 2 (`@SchemaMapping`) | Step 4 (`@BatchMapping`) |
|---|---|
| `List<Movie> directedMovies(Person person)` | `Map<Person, List<Movie>> directedMovies(List<Person> people)` |
| Called once per person | Called once per query |
| Returns one person's movies | Returns every person's movies, keyed by person |

Under the hood, Spring registers a `DataLoader` for the field. During execution, GraphQL no longer resolves `directedMovies` immediately: it collects every `Person` that needs the field, dispatches the whole batch to our method in one call, and distributes the results from the returned map. People with no entry in the map receive the `getOrDefault` fallback, an empty list.

Restart and run the exact query from step 3 again:

- The response time drops from around **eleven seconds to around two**: one paused call for the `people` page, one for the entire batch.
- The SQL log shows two `select` statements instead of eleven.
- The application log contains a single line: `Batch fetching directed movies for 10 people`.

The schema did not change. Clients notice nothing except the speed: batch loading is a pure server-side optimization, which is exactly why it belongs in every GraphQL API from the start.

### The same trap from step 1

One more field needs this treatment. In step 1, `directors` resolved through the entity getter with no code at all. Convenient, but look at what it costs: each movie lazy-loads its own director list, one SQL query per movie. The same N+1, triggered by JPA instead of our resolver. The batch version follows the exact pattern, backed by `PersonService.findDirectorsByMovieIds`, which is already in the starter:

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@BatchMapping
Map<Movie, List<Person>> directors(List<Movie> movies) {
    List<Long> movieIds = movies.stream().map(Movie::getId).toList();
    Map<Long, List<Person>> directorsByMovieId = personService.findDirectorsByMovieIds(movieIds);
    return movies.stream()
            .collect(Collectors.toMap(movie -> movie,
                    movie -> directorsByMovieId.getOrDefault(movie.getId(), List.of())));
}
```

A movie list with directors now costs two service calls in total, regardless of page size. The rule of thumb this leaves us with: the default resolver is fine for scalar fields; for list relationships, batch from the start.

## 5. Junction entities: modeling the cast

Movies also have actors, and this relationship is richer than `directors`: an acting credit carries data of its own, the character name. A plain many-to-many link has nowhere to put that data. The database therefore models it as a **junction entity**: `MovieCast`, a row that connects one `Movie` and one `Person` and holds the `characterName` for exactly that pairing.

In GraphQL we expose the junction the same way, as a type of its own. Add it to the schema:

`src/main/resources/graphql/schema.graphqls`

```graphql
type MovieCast {
    id: ID!
    characterName: String!
    person: Person!
    movie: Movie!
}
```

This type is worth a pause. It is the first type in our schema whose entire purpose is to describe a relationship: its `person` and `movie` fields are object types, not scalars, so a `MovieCast` is a node you can navigate *through*. From a movie, through its cast, to an actor, and onward to everything that actor connects to. This is nested resolution at full strength, and it is what the "graph" in GraphQL refers to.

On its own the type is not reachable yet: no field anywhere returns a `MovieCast`. Wiring it in from both directions is your job in the exercises.

## Exercises

The data access is already in place for both directions: `PersonService.findCastByMovieIds` and `PersonService.findMovieCastCreditsByPersonIds` return id-keyed maps, exactly like the method we used in step 4. You write the schema fields and the batch resolvers.

### Exercise 1: cast on Movie

Add a `cast` field on `Movie` returning the new `MovieCast` type, then resolve it with a `@BatchMapping` backed by `PersonService.findCastByMovieIds`. When it works, movie detail pages in the frontend show the cast with character names.

Before reaching for the resolver, consider: the `Movie` entity has a `getCast()` method, so the default resolver would make this field work with no code at all. It would also lazy-load one cast list per movie, which is the N+1 problem from step 3 in disguise. That is why we batch it from the start.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
type Movie {
    # ...existing fields...
    cast: [MovieCast!]!
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
import com.graphqlguy.moviedb.movie.MovieCast;

@BatchMapping
Map<Movie, List<MovieCast>> cast(List<Movie> movies) {
    List<Long> movieIds = movies.stream().map(Movie::getId).toList();
    Map<Long, List<MovieCast>> castByMovieId = personService.findCastByMovieIds(movieIds);
    return movies.stream()
            .collect(Collectors.toMap(movie -> movie,
                    movie -> castByMovieId.getOrDefault(movie.getId(), List.of())));
}
```

Note that the `person` and `movie` fields of `MovieCast` need no resolvers: the entity has the matching getters, and both objects are already loaded by the fetch join inside the service.

</details>

### Exercise 2: acting credits on Person

Now the reverse direction: a person's acting credits. Add a `movieCastCredits` field to `Person` and resolve it with a `@BatchMapping` backed by `PersonService.findMovieCastCreditsByPersonIds`. When it works, a person's profile page in the frontend shows their acting roles next to the movies they directed, and people like Clint Eastwood appear in both sections.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
type Person {
    # ...existing fields...
    """This person's acting credits in movies"""
    movieCastCredits: [MovieCast!]!
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@BatchMapping
Map<Person, List<MovieCast>> movieCastCredits(List<Person> people) {
    List<Long> personIds = people.stream().map(Person::getId).toList();
    Map<Long, List<MovieCast>> creditsByPersonId = personService.findMovieCastCreditsByPersonIds(personIds);
    return people.stream()
            .collect(Collectors.toMap(person -> person,
                    person -> creditsByPersonId.getOrDefault(person.getId(), List.of())));
}
```

</details>
