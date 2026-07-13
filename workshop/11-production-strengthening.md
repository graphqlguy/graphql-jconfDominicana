# Class 11: Production Strengthening

The API is feature complete. This class is about the things a GraphQL service needs before it faces real traffic: a guard against abusive queries, visibility into what is slow, a way to correlate a response back to its request, and the browser-facing configuration that lets the frontend talk to it. None of this changes the schema; it hardens what is already there.

By the end of this class, you will:

- Write a custom **`Instrumentation`** that hooks into the execution pipeline
- Reject abusively deep queries and log slow resolvers
- Attach a correlation id to every request with a `WebGraphQlInterceptor`
- Configure CORS and externalize the tuning knobs

## 1. Instrumentation: hooking the execution pipeline

GraphQL executes a query in well-defined phases: parse, validate, then resolve each field. `graphql.execution.instrumentation.Instrumentation` lets you hook those phases. Spring for GraphQL picks up any `Instrumentation` bean automatically, so an instrumentation is just a `@Component` extending `SimplePerformantInstrumentation` and overriding the hooks you care about.

Everything here is driven by one small properties record, bound from `application.yaml` and picked up by the `@ConfigurationPropertiesScan` already on the application class.

`src/main/java/com/graphqlguy/moviedb/instrumentation/InstrumentationProperties.java` (new)

```java
package com.graphqlguy.moviedb.instrumentation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "graphql")
public record InstrumentationProperties(
        @DefaultValue("10") int maxQueryDepth,
        @DefaultValue("100") long slowResolverThresholdMs) {}
```

## 2. Rejecting abusively deep queries

Because a GraphQL schema has cycles (a movie has cast, a cast member has credits, a credit has a movie), a client can nest a query arbitrarily deep and force the server to do enormous work from a tiny request. This is the classic GraphQL denial-of-service vector. We reject any query past a configured depth, before it executes, by hooking `instrumentDocumentAndVariables` (which runs on the parsed document).

`src/main/java/com/graphqlguy/moviedb/instrumentation/QueryDepthInstrumentation.java` (new)

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class QueryDepthInstrumentation extends SimplePerformantInstrumentation {

    private final InstrumentationProperties properties;

    @Override
    public DocumentAndVariables instrumentDocumentAndVariables(
            DocumentAndVariables documentAndVariables,
            InstrumentationExecutionParameters parameters,
            InstrumentationState state) {

        int maxDepth = properties.maxQueryDepth();
        Document document = documentAndVariables.getDocument();

        Map<String, FragmentDefinition> fragments = document.getDefinitions().stream()
                .filter(FragmentDefinition.class::isInstance)
                .map(FragmentDefinition.class::cast)
                .collect(Collectors.toMap(FragmentDefinition::getName, f -> f));

        int depth = calculateDepth(document, fragments);
        if (depth > maxDepth) {
            log.warn("Query rejected: depth {} exceeds maximum {}", depth, maxDepth);
            throw new AbortExecutionException(
                    String.format("Query depth %d exceeds maximum allowed depth of %d", depth, maxDepth));
        }
        return documentAndVariables;
    }

    // Walks the selection sets, following inline fragments and fragment spreads,
    // returning the deepest field nesting. (See the full file for the recursion.)
    private int calculateDepth(Document document, Map<String, FragmentDefinition> fragments) { /* ... */ }
    private int depthOf(SelectionSet selectionSet, Map<String, FragmentDefinition> fragments,
                        Set<String> visitedFragments) { /* ... */ }
}
```

Two details worth noting: the walk resolves **fragment spreads** (so a query cannot hide depth inside a fragment), and it guards against a fragment cycle with a `visited` set, because this hook runs before validation has ruled cycles out. Throwing `AbortExecutionException` stops execution and returns the message as a GraphQL error.

> **Why depth 15?** The default in `application.yaml` is 15, not 10. The standard introspection query GraphiQL runs to build its docs and autocomplete is itself about 13 levels deep, so a lower limit would break tooling. Set the ceiling above your own legitimate queries and introspection, not at some tidy round number.

## 3. Logging slow resolvers

For observability, we time every field fetch and log the ones that cross a threshold. The `beginFieldFetching` hook gives us a per-field context whose `onCompleted` fires when the field resolves.

`src/main/java/com/graphqlguy/moviedb/instrumentation/FieldTimingInstrumentation.java` (new)

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class FieldTimingInstrumentation extends SimplePerformantInstrumentation {

    private final InstrumentationProperties properties;

    @Override
    public FieldFetchingInstrumentationContext beginFieldFetching(
            InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
        long startNanos = System.nanoTime();
        String fieldPath = parameters.getEnvironment().getExecutionStepInfo().getPath().toString();

        return new FieldFetchingInstrumentationContext() {
            @Override public void onDispatched() { }
            @Override public void onCompleted(Object result, Throwable error) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                if (durationMs >= properties.slowResolverThresholdMs()) {
                    log.warn("Slow resolver: {} took {}ms", fieldPath, durationMs);
                }
            }
        };
    }
}
```

This is a natural companion to the N+1 discussion from earlier classes: raise `demo.latency`, run a query over a list, and the log names the exact field paths that are slow. It is how you would notice a missing `@BatchMapping` in production.

## 4. A correlation id per request

When a request fails in production, you need to tie the log lines to the response the client saw. A `WebGraphQlInterceptor` wraps the whole request: we mint a request id, put it in the GraphQL context (so resolvers and instrumentation can read it), and echo it back as a response header.

`src/main/java/com/graphqlguy/moviedb/config/RequestContextInterceptor.java` (new)

```java
@Component
public class RequestContextInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String requestId = UUID.randomUUID().toString();
        request.configureExecutionInput((executionInput, builder) ->
                builder.graphQLContext(ctx -> ctx.put("requestId", requestId)).build());

        return chain.next(request).doOnNext(response ->
                response.getResponseHeaders().add("X-Request-Id", requestId));
    }
}
```

Every response now carries an `X-Request-Id` header, and any resolver can read the same id from the context to stamp its own logs.

## 5. Production configuration

The last pieces live in `application.yaml`.

```yaml
spring:
  graphql:
    cors:
      allowed-origins: http://localhost:5173
      allow-credentials: true
  threads:
    virtual:
      enabled: true

graphql:
  max-query-depth: 15
  slow-resolver-threshold-ms: 100
```

- **CORS**: the browser will not let the React app on `:5173` call the API on `:8080` without the server explicitly allowing that origin. Spring for GraphQL has dedicated CORS properties for the GraphQL endpoint.
- **Virtual threads**: on Java 21, enabling virtual threads lets blocking work (JPA queries, the external TMDB and countries calls) scale without a large thread pool.
- The two `graphql.*` keys bind to `InstrumentationProperties`.

Restart, and confirm the guards are live. A normal query returns an `X-Request-Id` header. A deliberately deep query is rejected:

```graphql
{ movie(id: 1) { cast { person { movieCastCredits { movie {
  cast { person { movieCastCredits { movie {
  cast { person { movieCastCredits { movie {
  cast { person { movieCastCredits { movie { title }}}}}}}}}}}}}}}} }
```

returns `Query depth 18 exceeds maximum allowed depth of 15`.

## Recap

- An `Instrumentation` bean hooks the execution pipeline; Spring registers it automatically.
- Depth limiting is essential for a public GraphQL API, because schema cycles let a tiny request demand huge work; reject past a ceiling set above introspection depth.
- Field timing turns "something is slow" into "this field path is slow", which is how you catch a missing batch loader in production.
- A `WebGraphQlInterceptor` gives every request a correlation id, in the context and on the response.
- CORS, virtual threads, and the instrumentation thresholds are configuration, not code.

---

## Exercises: tightening the API

Building the feature set, we moved fast and left a few things half-done. Production is where they get finished. Each of these is small, and each uses something you already learned.

### 1. Extend input validation

Class 6 introduced the `@Range`, `@Size`, and `@Pattern` directives, but only `CreateMovieInput` got them. Untrusted input enters in more places. Apply the directives to:

- `RegisterInput` — a real minimum length on `username`/`password`, a maximum on `email`, and an email `@Pattern`.
- the `movies` and `people` query pagination arguments — `page`/`size` `@Range` (the `tvShows` query already has them).
- `UpdateMovieInput` — the same limits `CreateMovieInput` has.

<details>
<summary>Solution</summary>

```graphql
directive @Pattern(regexp: String!, message: String = "graphql.validation.Pattern.message") on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION

input RegisterInput {
    username: String! @Size(min: 3, max: 50)
    email: String! @Size(max: 120) @Pattern(regexp: "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    password: String! @Size(min: 6, max: 100)
}

type Query {
    movies(filter: MovieFilter, page: Int = 0 @Range(min: 0), size: Int = 10 @Range(min: 1, max: 100), sort: MovieSort): MoviePage!
    people(page: Int = 0 @Range(min: 0), size: Int = 20 @Range(min: 1, max: 100)): PersonPage!
}

input UpdateMovieInput {
    id: ID!
    title: String @Size(min: 1, max: 200)
    releaseYear: Int @Range(min: 1888, max: 2100)
    genre: Genre
    rating: Float @Range(min: 0, max: 10)
    runtime: Int @Range(min: 1, max: 600)
    plot: String @Size(max: 2000)
    posterUrl: String @Size(max: 500)
    tmdbId: Int
}
```

The `@Pattern` directive must be declared once, alongside `@Range` and `@Size`. No Java changes: the wiring from Class 6 enforces all of it.

</details>

### 2. Handle duplicate reviews gracefully

Try reviewing the same movie twice: the service throws `DuplicateReviewException`, but with no handler it falls through to the catch-all and surfaces as an opaque 500. Add a handler so it returns a clean `BAD_REQUEST`, the way the other domain exceptions do (Class 5).

<details>
<summary>Solution</summary>

`GlobalExceptionHandler`:

```java
@GraphQlExceptionHandler
public GraphQLError handleDuplicateReview(DuplicateReviewException ex, DataFetchingEnvironment env) {
    return GraphqlErrorBuilder.newError(env)
            .message(ex.getMessage())
            .errorType(ErrorType.BAD_REQUEST)
            .build();
}
```

</details>

### 3. Make search global

Search should find everything. Class 7 built a `Movie | Person` union; TV shows already carry the `SearchResult` marker in anticipation. Add `TvShow` to the union and have the search resolver include shows, so one box searches the whole catalogue.

<details>
<summary>Solution</summary>

```graphql
union SearchResult = Movie | Person | TvShow
```

`SearchController`:

```java
private final TvShowService tvShowService;

@QueryMapping
List<SearchResult> search(@Argument String query) {
    List<SearchResult> results = new ArrayList<>();
    results.addAll(movieService.searchByTitle(query));
    results.addAll(personService.searchByName(query));
    results.addAll(tvShowService.searchByTitle(query));
    return results;
}
```

Spring resolves each result's concrete type by class name, exactly as before; the frontend's search page renders a TV Shows section as soon as the union includes them.

</details>

### 4. Expose the request id to clients on errors

The interceptor puts `requestId` in the GraphQL context. Surface it in the errors your API returns, so a client reporting a bug can quote the id.

<details>
<summary>Hint</summary>

The exception handler receives a `DataFetchingEnvironment`, from which `env.getGraphQlContext().get("requestId")` reads the value the interceptor stored. Add it to each error via `GraphqlErrorBuilder.newError(env).extensions(Map.of("requestId", requestId, ...))`, giving every error a handle back to the exact request in your logs.

</details>
