# Class 3: Mutations

> **User story** · As the product owner, I want my team to be able to add, correct, and remove titles themselves, so that the catalogue stays accurate without a developer on call.

So far our API only reads. Time to change data. In GraphQL, every write goes through a **mutation**: the counterpart of REST's POST, PUT, and DELETE, except that all mutations travel through the same `/graphql` endpoint, and the operation type declares the intent.

Two things are worth understanding before we write one:

- **Nothing technically stops you from changing data inside a query resolver.** GraphQL will not complain. It is still a mistake, for the same reason mutating state in a REST GET handler is a mistake: clients, caches, and tools all assume that reads are safe to repeat, retry, and prefetch. The query/mutation split is a contract with your consumers, and the schema makes that contract explicit.
- **Top-level mutation fields run sequentially.** Within a single request, GraphQL resolves query fields in whatever order it likes, potentially in parallel. Mutation fields are guaranteed to run one after another, in the order written. Send two mutations in one request and the second sees the effects of the first.

By the end of this class, you will:

- Add the `Mutation` root type and write resolvers with `@MutationMapping`
- Design response types that report success and failure as data
- Model a destructive operation safely with a flag and a structured error enum
- Use input types for create and update operations, including partial updates
- Distinguish "field omitted" from "field explicitly null" with `ArgumentValue`, and derive the write policy from the schema's nullability

## 1. The first mutation: deleteMovie

We start with delete because it is the smallest possible mutation: one argument in, one result out. Schema first, as always. `Mutation` is the second root type, next to `Query`:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Mutation {
    deleteMovie(id: ID!): DeleteMovieResponse!
}

"""Result of deleting a movie."""
type DeleteMovieResponse {
    success: Boolean!
    message: String!
    """ID of the deleted movie, or null if nothing was deleted."""
    deletedId: ID
}
```

Note what `deleteMovie` returns. It could return `Boolean`, but a dedicated response type carries much more: did it work, what happened, and which id was affected. Deleting something that does not exist is then an ordinary answer (`success: false`), not an exception. We will build on this pattern throughout the workshop.

The Java side mirrors `@QueryMapping` exactly, with `@MutationMapping` binding the method to a `Mutation` field. The service already exists, and it is worth a look before you continue: `MovieService.deleteMovie` deletes the movie's reviews first, because the reviews table holds a foreign key to movies.

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
import org.springframework.graphql.data.method.annotation.MutationMapping;

@MutationMapping
DeleteMovieResponse deleteMovie(@Argument Long id) {
    return movieService.deleteMovie(id);
}
```

Restart and run your first mutation in GraphiQL. Note the `mutation` keyword: until now, GraphiQL let us omit `query` because it is the default.

```graphql
mutation {
  deleteMovie(id: 11) {
    success
    message
    deletedId
  }
}
```

Run it twice. The first run deletes the movie; the second returns `success: false` with a message, exactly as designed. In the frontend, movie pages now show a Delete button.

> [!NOTE]
> The database is in-memory and reseeded on startup, so restarting the application restores everything you delete during this class.

## 2. A destructive operation done carefully: deletePerson

Deleting a person is harder: people are referenced by movies (as directors and cast). Silently cascading the delete would be surprising; failing with an opaque error would be unhelpful. We do neither. The API refuses by default and reports *why* as structured data, and the caller can explicitly insist with a `force` flag:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Mutation {
    # ...existing fields...
    "Deletes a person. With force: true, all movie/TV credits are unlinked first; otherwise a linked person yields a structured error."
    deletePerson(id: ID!, force: Boolean! = false): DeletePersonResponse!
}

type DeletePersonResponse {
    success: Boolean!
    error: String
    deletedId: ID
}
```

Three details to notice:

- `force: Boolean! = false` gives the argument a default, like `page: Int = 0` in class 1. Callers who do not care about `force` never see it.
- The `error` field reports *why* the delete was refused, as data. For now it is a plain `String`; in the error-handling class we upgrade it to an enum so clients can branch on values like `LINKED_TO_MOVIE` programmatically instead of matching strings.
- `success`, `error`, `deletedId`: the response type again reports outcomes as data.

The service (`PersonService.delete`) contains the interesting logic: without `force` it checks for links and returns the matching error; with `force` it unlinks every credit first. The controller stays thin:

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@MutationMapping
DeletePersonResponse deletePerson(@Argument Long id, @Argument boolean force) {
    return personService.delete(id, force);
}
```

Try it on a person with movies, for example Christopher Nolan:

```graphql
mutation {
  deletePerson(id: 1) {
    success
    error
    deletedId
  }
}
```

The response is `success: false, error: LINKED_TO_MOVIE`, and nothing was deleted. Add `force: true` to the arguments and run it again: the credits are unlinked and the person is gone. Restart the application to bring Nolan back.

## 3. Creating data: createMovie

Creates take more than one value, and we already know the tool for structured arguments from the `movies` filter in class 1: an **input type**. One field per column the client may set; the server owns the id.

`src/main/resources/graphql/schema.graphqls`

```graphql
type Mutation {
    # ...existing fields...
    createMovie(input: CreateMovieInput!): Movie!
}

"""Input for creating a movie."""
input CreateMovieInput {
    title: String!
    releaseYear: Int!
    genre: Genre!
    """Rating from 0.0 to 10.0"""
    rating: Float
    """Runtime in minutes"""
    runtime: Int
    plot: String
    posterUrl: String
    tmdbId: Int
}
```

The mutation returns `Movie!`, the full object type. That is deliberate: the client creates and reads back in a single round trip, selecting whichever fields it needs, including server-generated ones like `id`.

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
@MutationMapping
Movie createMovie(@Argument CreateMovieInput input) {
    return movieService.createMovie(input);
}
```

The matching `CreateMovieInput` Java record is already in the starter; Spring maps the GraphQL input onto it by field name, the same mechanism as `MovieFilter` in class 1.

```graphql
mutation {
  createMovie(input: {
    title: "The Bridges of Madison County"
    releaseYear: 1995
    genre: DRAMA
    rating: 7.6
    runtime: 135
  }) {
    id
    title
    releaseYear
  }
}
```

The response contains the new movie with its generated `id`, and the movie appears in the frontend's grid after a refresh.

## 4. Partial updates: updateMovie

Updates raise a design question creates do not have: what does it mean to leave a field out? Our convention for now: **every field except `id` is optional, and an omitted field keeps its current value.**

`src/main/resources/graphql/schema.graphqls`

```graphql
type Mutation {
    # ...existing fields...
    updateMovie(input: UpdateMovieInput!): Movie!
}

"""Input for updating a movie; omitted fields are left unchanged."""
input UpdateMovieInput {
    id: ID!
    title: String
    releaseYear: Int
    genre: Genre
    """Rating from 0.0 to 10.0"""
    rating: Float
    """Runtime in minutes"""
    runtime: Int
    plot: String
    posterUrl: String
    tmdbId: Int
}
```

Compare it with `CreateMovieInput`: same fields, but `title`, `releaseYear`, and `genre` lost their `!`. The service (`MovieService.updateMovie`) loads the movie and applies only the non-null values.

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
@MutationMapping
Movie updateMovie(@Argument UpdateMovieInput input) {
    return movieService.updateMovie(input);
}
```

Change a single field and read the result back:

```graphql
mutation {
  updateMovie(input: { id: 29, rating: 9.0 }) {
    title
    rating
    plot
  }
}
```

Only `rating` changed; `title` and `plot` kept their values. With `createMovie` and `updateMovie` in place, the frontend's Add Movie page and the Edit button on movie pages are now fully functional.

## 5. Absent or null: upgrading to ArgumentValue

Our convention has a blind spot. Try to *clear* a movie's plot by sending an explicit `null`:

```graphql
mutation {
  updateMovie(input: { id: 29, plot: null }) {
    title
    plot
  }
}
```

The plot survives. To our Java record, "the client sent `plot: null`" and "the client did not send `plot`" are the same thing: a `null` field. The information was there in the request, and the mapping to the record destroyed it.

GraphQL itself distinguishes the two, and Spring for GraphQL exposes the distinction through the `ArgumentValue<T>` wrapper, which carries three possible states: omitted, explicitly `null`, or a value. Upgrade the input record first:

`src/main/java/com/graphqlguy/moviedb/movie/UpdateMovieInput.java`

```java
import org.springframework.graphql.data.ArgumentValue;

public record UpdateMovieInput(
        Long id,
        ArgumentValue<String> title,
        ArgumentValue<Integer> releaseYear,
        ArgumentValue<Genre> genre,
        ArgumentValue<Double> rating,
        ArgumentValue<Integer> runtime,
        ArgumentValue<String> plot,
        ArgumentValue<String> posterUrl,
        ArgumentValue<Integer> tmdbId) {
}
```

Now the service can see all three states, which raises a policy question: *should* every field be clearable? No. The schema already answers it: `Movie.title` is `String!`, so a null title must never reach the database, while `plot` is nullable and clearing it is legitimate. We encode exactly that policy with two helpers in `MovieService`. They replace the starter's existing null-check `applyIfPresent` helper — delete it as you add these; its plain-value signature cannot tell the three states apart:

`src/main/java/com/graphqlguy/moviedb/movie/MovieService.java`

```java
import org.springframework.graphql.data.ArgumentValue;

// Required fields: a provided value replaces the old one; null or omitted leaves it unchanged.
private <T> void applyIfPresent(final ArgumentValue<T> arg, final Consumer<T> setter) {
    if (arg.isPresent()) {
        setter.accept(arg.value());
    }
}

// Optional fields: an explicit null clears the value; an omitted field leaves it unchanged.
private <T> void applyIfProvided(final ArgumentValue<T> arg, final Consumer<T> setter) {
    if (!arg.isOmitted()) {
        setter.accept(arg.value());
    }
}
```

And split the fields in `updateMovie` accordingly: the required ones keep `applyIfPresent`, the optional ones switch to `applyIfProvided`:

```java
applyIfPresent(input.title(), movie::setTitle);
applyIfPresent(input.releaseYear(), movie::setReleaseYear);
applyIfPresent(input.genre(), movie::setGenre);
applyIfProvided(input.rating(), movie::setRating);
applyIfProvided(input.runtime(), movie::setRuntime);
applyIfProvided(input.plot(), movie::setPlot);
applyIfProvided(input.posterUrl(), movie::setPosterUrl);
applyIfProvided(input.tmdbId(), movie::setTmdbId);
```

Restart and verify all three behaviors:

- `plot: null` now clears the plot.
- Omitting `plot` leaves it unchanged, as before.
- `title: null` is ignored: the schema declares `title` non-null, and the service enforces the same policy on writes.

The rule this leaves us with: **the schema's nullability is not just documentation, it is the write policy**, and `ArgumentValue` is what lets the service enforce it precisely.

## Exercises

The same two patterns, applied to people. The services (`PersonService.createPerson`, `PersonService.updatePerson`) are already in the starter.

### Exercise 1: createPerson

Add a `createPerson(input: CreatePersonInput!): Person!` mutation. A person has a required `name` and optional `birthYear` and `countryCode`. One extra: the starter's `CreatePersonInput` record carries Bean Validation annotations (`@NotBlank`, `@Size`, `@Min`); annotate the controller argument with `@Valid` to activate them and see what an invalid input produces in GraphiQL.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
type Mutation {
    # ...existing fields...
    createPerson(input: CreatePersonInput!): Person!
}

input CreatePersonInput {
    name: String!
    birthYear: Int
    countryCode: String
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.MutationMapping;

@MutationMapping
Person createPerson(@Argument @Valid CreatePersonInput input) {
    return personService.createPerson(input);
}
```

</details>

### Exercise 2: updatePerson

Add an `updatePerson(input: UpdatePersonInput!): Person!` mutation and apply the full step-5 treatment: upgrade `UpdatePersonInput` to `ArgumentValue` fields and give `PersonService` the same two helpers (again replacing the existing null-check `applyIfPresent`). Mind the policy: `Person.name` is `String!` in the schema, so it must never be cleared, while `birthYear` and `countryCode` are fair game for an explicit `null`. As the finishing touch, make the service reject a name that was *provided* as null or blank (hint: `isOmitted()`), and confirm in GraphiQL that `updatePerson(input: { id: 1, name: null })` returns an error instead of silently doing nothing.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
type Mutation {
    # ...existing fields...
    updatePerson(input: UpdatePersonInput!): Person!
}

input UpdatePersonInput {
    id: ID!
    name: String
    birthYear: Int
    countryCode: String
}
```

`src/main/java/com/graphqlguy/moviedb/person/UpdatePersonInput.java`

```java
import org.springframework.graphql.data.ArgumentValue;

public record UpdatePersonInput(
        Long id,
        ArgumentValue<String> name,
        ArgumentValue<Integer> birthYear,
        ArgumentValue<String> countryCode) {
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
@MutationMapping
Person updatePerson(@Argument UpdatePersonInput input) {
    return personService.updatePerson(input);
}
```

`src/main/java/com/graphqlguy/moviedb/person/PersonService.java` (inside `updatePerson`, plus the two helpers from step 5):

```java
if (!input.name().isOmitted() && StringUtils.isBlank(input.name().value())) {
    throw new InvalidInputException("name", "Name can't be blank");
}
// ...
applyIfPresent(input.name(), person::setName);
applyIfProvided(input.birthYear(), person::setBirthYear);
applyIfProvided(input.countryCode(), person::setCountryCode);
```

The guard fires only when `name` was actually provided: an omitted name is fine (leave it unchanged), a provided null or blank one is a client error. That distinction is impossible with a plain nullable field, and it is the whole point of `ArgumentValue`.

Note that we are duplicating the two small helpers that already live in `MovieService`. For now, keep it simple and copy them into `PersonService`; if you prefer, extract them into a shared helper class instead.

</details>

## Recap

- Mutations are GraphQL's writes: same endpoint, explicit intent, and top-level mutation fields execute strictly in order.
- Changing data from a query resolver is possible and wrong, for the same reason a mutating GET is wrong.
- Response types (`success`, `message`/`error`, `deletedId`) report expected failures as data; enums make those failures machine-readable.
- Input types drive creates and updates; for updates, omitted fields keep their values.
- `ArgumentValue` makes omitted, null, and value three distinct states; the schema's nullability decides which fields an explicit `null` may clear.

Next up: what happens when things actually go wrong, and how to turn GraphQL's default error response into something clients can work with.
