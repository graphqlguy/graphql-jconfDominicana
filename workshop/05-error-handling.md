# Class 5: Error Handling

GraphQL handles failure differently from REST. There is no 404 or 500 status code per operation; a GraphQL response is almost always `HTTP 200`, and problems are reported inside the response body. Crucially, a single response can contain **both** a `data` field and an `errors` array: some fields resolve while others fail, and the client receives everything that succeeded alongside a precise description of what did not.

This gives us two distinct tools, and this class covers both:

- **Errors as data**: expected, business-level outcomes modeled directly in the schema. We already use this. `DeleteMovieResponse` and `DeletePersonResponse` report `success` and a reason as ordinary fields the client can query.
- **The errors array**: for exceptions thrown during execution. This is what we add today.

By the end of this class, you will:

- Map domain exceptions to meaningful GraphQL errors with a single handler
- Enrich errors with a classification and structured `extensions`
- Sharpen an errors-as-data field with a typed enum

## 1. The problem: opaque exceptions

The starter already contains dedicated exceptions, thrown by the service layer: `EntityNotFoundException` (carrying the entity type) and `InvalidInputException` (carrying the field name). In classes 3 and 4 we saw what happens to them when they reach the client:

```graphql
query { person(id: 99999) { name } }
```

```json
{ "errors": [ { "message": "INTERNAL_ERROR for a3d8...", "extensions": { "classification": "INTERNAL_ERROR" } } ], "data": { "person": null } }
```

Every exception, whatever its meaning, becomes an opaque `INTERNAL_ERROR`. The client cannot tell "not found" from "invalid input" from a genuine server fault, and the useful information the exception carried is discarded.

## 2. The exception handler

Spring for GraphQL maps exceptions to errors through methods annotated with `@GraphQlExceptionHandler`, collected in a `@ControllerAdvice` class. Each method receives one exception type and returns a `GraphQLError`.

`src/main/java/com/graphqlguy/moviedb/exception/GlobalExceptionHandler.java`

```java
package com.graphqlguy.moviedb.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleEntityNotFound(final EntityNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of("entityType", ex.getEntityType()))
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidInput(final InvalidInputException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("field", ex.getField()))
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleAccessDenied(AccessDeniedException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message("You are not authorized to perform this action")
                .errorType(ErrorType.FORBIDDEN)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConstraintViolation(ConstraintViolationException ex, DataFetchingEnvironment env) {
        ConstraintViolation<?> first = ex.getConstraintViolations().iterator().next();
        String field = first.getPropertyPath().toString();
        return GraphqlErrorBuilder.newError(env)
                .message(field + " " + first.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("field", field))
                .build();
    }

    // Safety net for database constraints (foreign keys, unique constraints);
    // this is Spring's DataAccessException, not the Jakarta validation one above.
    @GraphQlExceptionHandler
    public GraphQLError handleDataIntegrityViolation(final DataIntegrityViolationException ex, DataFetchingEnvironment env) {
        log.warn("Data integrity violation at path={}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                .message("The request conflicts with existing data and could not be completed")
                .errorType(ErrorType.BAD_REQUEST)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnhandled(final Exception ex, DataFetchingEnvironment env) {
        String reference = UUID.randomUUID().toString();
        log.error("Unhandled exception, reference={}, path={}", reference, env.getExecutionStepInfo().getPath(), ex);
        return GraphqlErrorBuilder.newError(env)
                .message("An unexpected error occurred while processing the request. Reference: " + reference)
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of("reference", reference))
                .build();
    }
}
```

One method per exception type, each mapping to a `GraphQLError`. The important ideas:

- **`errorType`** sets the error's `classification`, GraphQL's equivalent of an HTTP status. `NOT_FOUND`, `BAD_REQUEST`, `FORBIDDEN`, and `INTERNAL_ERROR` let clients branch on the kind of failure without parsing messages. (`handleAccessDenied` is what produced the clean `FORBIDDEN` we saw in class 4.)
- **`extensions`** attach structured, machine-readable detail: the `entityType` that was missing, the `field` that was invalid. Clients read these instead of scraping the message string.
- **The catch-all** `handleUnhandled` is the safety net. Any exception without a specific handler still becomes a clean `INTERNAL_ERROR`, but the real cause is logged against a random `reference` id that is also returned to the client. Support can find the exact stack trace from the reference without ever exposing internals.

Restart and run the same query:

```graphql
query { person(id: 99999) { name } }
```

```json
{
  "errors": [ {
    "message": "Entity Person not found for id: 99999",
    "path": ["person"],
    "extensions": { "entityType": "Person", "classification": "NOT_FOUND" }
  } ],
  "data": { "person": null }
}
```

The opaque failure is now a typed, described `NOT_FOUND`.

## 3. Errors as data: a typed enum

The other half of error handling needs no exceptions at all. Recall `deletePerson` from class 3: a linked person is not an exceptional condition, it is an expected outcome, so we returned it as data in `DeletePersonResponse`. At the time its `error` field was a plain `String`. Now we give it a precise type:

`src/main/resources/graphql/schema.graphqls`

```graphql
type DeletePersonResponse {
    success: Boolean!
    error: DeletePersonError
    deletedId: String
}

enum DeletePersonError {
    LINKED_TO_MOVIE
    LINKED_TO_TV_SHOW
}
```

The Java side already returns these values (the `DeletePersonError` enum exists in the starter); we are only making the schema state the contract. The difference for clients is significant: an enum is validated and introspectable, so a consumer can branch on `LINKED_TO_MOVIE` with confidence rather than string-matching a free-form message.

This is the decision at the heart of the class. **Expected outcomes belong in the schema as data; unexpected failures belong in the errors array.** A "person is linked" result is data; a "database is unreachable" fault is an error.

## 4. Partial responses: data and errors together

The exception handlers above turn a thrown exception into an error and null out the field that threw. But a resolver can go further and return **both** a value and one or more errors on purpose. This is the GraphQL feature with no REST equivalent, and one small query demonstrates it.

Consider a batch lookup by id. Some ids exist, some do not. Rather than failing the whole call, we want to return the movies that were found and report the ones that were not. Spring for GraphQL supports this through `DataFetcherResult`, which wraps a value together with a list of errors:

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    moviesByIds(ids: [ID!]!): [Movie]
}
```

`src/main/java/com/graphqlguy/moviedb/movie/MovieController.java`

```java
@QueryMapping
public DataFetcherResult<List<Movie>> moviesByIds(@Argument List<Long> ids, DataFetchingEnvironment env) {
    List<Movie> found = movieService.findByIds(ids);
    Set<Long> foundIds = found.stream().map(Movie::getId).collect(Collectors.toSet());
    List<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();

    var result = DataFetcherResult.<List<Movie>>newResult().data(found);
    if (!missing.isEmpty()) {
        result.error(GraphqlErrorBuilder.newError(env)
                .message("Movies not found: " + missing)
                .errorType(ErrorType.NOT_FOUND)
                .build());
    }
    return result.build();
}
```

Restart and query a mix of real and missing ids:

```graphql
query { moviesByIds(ids: [1, 2, 999]) { id title } }
```

```json
{
  "errors": [ {
    "message": "Movies not found: [999]",
    "path": ["moviesByIds"],
    "extensions": { "classification": "NOT_FOUND" }
  } ],
  "data": {
    "moviesByIds": [
      { "id": "1", "title": "The Shawshank Redemption" },
      { "id": "2", "title": "The Godfather" }
    ]
  }
}
```

The found movies are in `data`; the missing id is reported in `errors`; both arrive in a single `HTTP 200` response. A REST endpoint would have to choose between `200` with a silently shortened list or `404` with nothing at all. GraphQL returns exactly what succeeded and exactly what did not.

## Recap

- A GraphQL response carries `data` and `errors` together; partial success is normal, and the transport stays `HTTP 200`.
- `@ControllerAdvice` plus `@GraphQlExceptionHandler` maps each exception to a `GraphQLError` with an `errorType` classification and structured `extensions`.
- A catch-all handler keeps genuine faults from leaking internals, logging them against a client-visible reference id.
- Expected outcomes are modeled as data (a typed enum here); only unexpected failures belong in the errors array.
