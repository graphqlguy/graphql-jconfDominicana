# Class 8: Custom Scalars and External APIs

> **User story** · As the product owner, I want the catalogue enriched beyond our own data, with countries, flags, and community ratings, so that our pages offer more than we store.

So far every value our API returns has been one of a handful of standard scalars, and every one of them has come from our own database. This class reaches past both of those boundaries. First we teach the schema new scalar types, so a field can be more specific than "just a string". Then we make our GraphQL server a *client* of another GraphQL server, resolving a field by calling out to a public API at query time.

By the end of this class, you will:

- Wire a custom scalar from the extended-scalars library and apply it to a field and its inputs
- Call an external GraphQL API from a resolver using `HttpSyncGraphQlClient`
- Cache external results and fail gracefully when the remote service is unavailable

## 1. Custom scalars

GraphQL ships with a deliberately small set of built-in scalars: `Int`, `Float`, `String`, `Boolean`, and `ID`. That is all the specification guarantees. In practice almost every real schema needs more: a date, a URL, an email address, a country code. You could model each of these as a plain `String`, but then the type says nothing and validates nothing. A custom scalar lets the schema state precisely what a value is, and lets the server parse, serialize, and validate it in one place.

Writing a correct scalar by hand is fiddly (you implement coercion in three directions: parsing literals, parsing variables, and serializing output). Because these types are so common, the community maintains a library of them, `graphql-java-extended-scalars`, which is where most projects get their scalars instead of writing their own. A selection of what it provides:

| Scalar | Purpose |
| --- | --- |
| `DateTime`, `Date`, `Time` | ISO-8601 temporal values |
| `URL` | a valid URL |
| `EmailAddress` | a valid email address |
| `CountryCode` | an ISO 3166-1 alpha-2 country code (`US`, `GB`, `DO`) |
| `Currency` | an ISO 4217 currency code |
| `PositiveInt`, `NonNegativeInt` | integers constrained by sign |
| `Long`, `BigDecimal` | numeric types beyond the built-ins |

We will use exactly one of these today, `CountryCode`, because we have a natural place for it: a `Person` has a country of origin, currently stored as a bare string.

### Add the dependency

`pom.xml`

```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-extended-scalars</artifactId>
    <version>24.0</version>
</dependency>
```

### Register the scalar

A scalar has to be registered with the runtime wiring, exactly like the validation directives we wired in the previous class. We already have a `RuntimeWiringConfigurer` for that; we add one line to it. A scalar is contributed with `.scalar(...)`, and `ExtendedScalars` exposes each library scalar as a constant.

`src/main/java/com/graphqlguy/moviedb/config/GraphQLConfig.java`

```java
import graphql.scalars.ExtendedScalars;

// inside runtimeWiringConfigurer(), on the returned wiring builder:
return wiringBuilder -> wiringBuilder
        .scalar(ExtendedScalars.CountryCode)
        .directiveWiring(new ValidationSchemaWiring(validationRules));
```

> [!TIP]
> If you did not build the validation class, you will not have this configurer yet. Create `GraphQLConfig` with a `@Bean RuntimeWiringConfigurer` that returns `wiringBuilder -> wiringBuilder.scalar(ExtendedScalars.CountryCode)` and nothing else.

### Declare and use the scalar

A custom scalar must be declared in the schema before it can be referenced, with a bare `scalar` line. Then we use it where a country code appears: add the field to the `Person` type (the entity has carried a country code all along; the schema just never exposed it), and in both person input types replace the existing `countryCode: String` with the scalar.

`src/main/resources/graphql/schema.graphqls`

```graphql
scalar CountryCode

type Person {
    # ...existing fields...
    """ISO 3166-1 alpha-2 country code"""
    countryCode: CountryCode
}

input CreatePersonInput {
    # ...existing fields...
    countryCode: CountryCode
}

input UpdatePersonInput {
    # ...existing fields...
    countryCode: CountryCode
}
```

Nothing changes on the Java side: the `Person` entity stores its country code as a plain `String`, and `CountryCode` serializes to and from a `String`. Restart the backend and read a person's code:

```graphql
query {
  person(id: 1) { name countryCode }
}
```

That returns `"GB"`, straight from the entity. The scalar's difference shows up on input. Try to create a person with a code that is not a valid ISO country:

```graphql
mutation {
  createPerson(input: { name: "Test Person", countryCode: "NOTACODE" }) {
    id
  }
}
```

The request is rejected before your controller ever runs, with a message like `is not a valid 'CountryCode' - Invalid ISO 3166-1 alpha-2 country code`. The scalar validates the value at the edge of the system, so nothing malformed reaches your service. This is the same "validate once, at the boundary" idea as the validation directives, applied at the level of the type itself.

## 2. Calling an external GraphQL API

A country code is useful, but a name, a flag, and a capital are more useful, and we do not have that data. Someone else does: `countries.trevorblades.com` is a public GraphQL API that knows everything about countries. Rather than copy its data into our database, we resolve `Person.country` by asking it, live, whenever a client requests that field.

The important idea here is that a GraphQL server can itself be a GraphQL *client*. Spring for GraphQL ships a client for exactly this, `HttpSyncGraphQlClient`, which sends a GraphQL query over HTTP and maps the response into your Java types.

### The Country type and record

First, what a country looks like, both in our schema and as a Java record to receive the response into.

`src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
    # ...existing queries...
    """Look up a country by its ISO 3166-1 alpha-2 code, e.g. CZ"""
    country(code: ID!): Country
}

type Person {
    # ...existing fields...
    """Country details resolved live from the external countries GraphQL API"""
    country: Country
}

type Country {
    "ISO 3166-1 alpha-2 code, e.g. CZ"
    code: ID!
    name: String!
    emoji: String
    capital: String
    currency: String
}
```

This adds two things. The standalone `country` query lets a client look up any country directly; the `country` field on `Person` is what enriches a person, and it is the one we resolve with our own code below.

`src/main/java/com/graphqlguy/moviedb/country/Country.java` (new)

```java
package com.graphqlguy.moviedb.country;

public record Country(String code, String name, String emoji, String capital, String currency) {}
```

### The service that calls out

The service builds an `HttpSyncGraphQlClient` pointed at the remote endpoint, holds the GraphQL query it will send, and exposes a single `findByCode`. Two details are worth pausing on: the timeouts and the cache.

`src/main/java/com/graphqlguy/moviedb/country/CountryService.java` (new)

```java
package com.graphqlguy.moviedb.country;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CountryService {

    private static final String COUNTRY_QUERY = """
        query Country($code: ID!) {
          country(code: $code) {
            code
            name
            emoji
            capital
            currency
          }
        }
        """;

    // Without timeouts the JDK client waits forever; a hung external call would block
    // every request resolving the same country code behind the cache's per-key lock.
    private final HttpSyncGraphQlClient graphQlClient = HttpSyncGraphQlClient.create(
            RestClient.builder()
                    .baseUrl("https://countries.trevorblades.com/")
                    .requestFactory(timeoutRequestFactory())
                    .build());

    private final Cache<String, Country> countryCache;

    public Country findByCode(String code) {
        return countryCache.get(code, this::fetchByCode);
    }

    private static JdkClientHttpRequestFactory timeoutRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    private Country fetchByCode(String code) {
        return graphQlClient.document(COUNTRY_QUERY)
                .variable("code", code)
                .retrieveSync("country")
                .toEntity(Country.class);
    }
}
```

`retrieveSync("country")` navigates into the `country` field of the response and `toEntity(Country.class)` maps it onto our record. The query we send is an ordinary GraphQL document; we are a client here, indistinguishable from any other.

**Timeouts.** A network call to a service you do not control can hang. The JDK HTTP client waits forever by default, so we set a connect and a read timeout. This is not optional politeness: a hung call would hold the cache's per-key lock and stall every request waiting on the same country.

**Cache.** Country data essentially never changes, and a list of twenty people might reference the same handful of codes. We cache aggressively so we call the external API once per code, not once per person. Add the cache bean.

`src/main/java/com/graphqlguy/moviedb/config/CacheConfig.java`

```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.graphqlguy.moviedb.country.Country;
import java.util.concurrent.TimeUnit;

@Bean
public Cache<String, Country> countryCache() {
    // Country data is effectively static; cache generously so person lists
    // don't refetch the same code from the external API on every request.
    return Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(300)
            .build();
}
```

> [!TIP]
> `CacheConfig` already exists in the starter (it holds a cache the TMDB scaffolding uses). Add the `countryCache` bean beside the existing one.

### The two resolvers

A standalone query, so you can look up any country directly:

`src/main/java/com/graphqlguy/moviedb/country/CountryController.java` (new)

```java
package com.graphqlguy.moviedb.country;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @QueryMapping
    Country country(@Argument String code) {
        return countryService.findByCode(code);
    }
}
```

And the field resolver that turns a `Person`'s code into a full `Country`. This is a `@SchemaMapping` on `Person`, the same nested-resolution pattern from class 2, except the data comes from an external API instead of a repository. Note the guard and the `try/catch`: a person may have no code, and the remote service may be down. Neither should break the whole query, so we return `null` and log rather than propagate.

`src/main/java/com/graphqlguy/moviedb/person/PersonController.java`

```java
import com.graphqlguy.moviedb.country.Country;
import com.graphqlguy.moviedb.country.CountryService;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import lombok.extern.slf4j.Slf4j;

// add @Slf4j to the class, and inject CountryService:
private final CountryService countryService;

@SchemaMapping(typeName = "Person")
Country country(Person person) {
    if (person.getCountryCode() == null) {
        return null;
    }
    try {
        return countryService.findByCode(person.getCountryCode());
    } catch (Exception e) {
        log.warn("Could not resolve country {} from external API: {}",
                person.getCountryCode(), e.getMessage());
        return null;
    }
}
```

Restart and ask a person for their full country:

```graphql
query {
  person(id: 1) {
    name
    countryCode
    country { name emoji capital currency }
  }
}
```

The `country` block is filled in by a live call to another GraphQL server: `{ "name": "United Kingdom", "emoji": "🇬🇧", "capital": "London", "currency": "GBP" }`. In the frontend, person cards now show a flag next to the name, resolved this same way.

> [!NOTE]
> **A note on N+1.** Because `country` resolves per person, a list of people triggers one external call per distinct code. The cache makes repeats free, but the first render of a diverse list still fans out. For an external dependency this is often acceptable, and the cache is the pragmatic mitigation. If it were not, `@BatchMapping` (class 2) is the tool, and the exercise below uses exactly that against TMDB.

## Recap

- The built-in scalars are minimal; `graphql-java-extended-scalars` supplies the common ones (`DateTime`, `URL`, `CountryCode`, and more).
- Register a scalar with `.scalar(ExtendedScalars.X)` on the runtime wiring, declare it with `scalar X`, then use it. It validates input at the boundary for free.
- A GraphQL server can be a GraphQL client: `HttpSyncGraphQlClient` sends a query to a remote endpoint and maps the result into your types.
- External calls need timeouts and benefit from caching; a resolver over an external service should fail soft (return `null`, log) rather than break the whole response.

---

## Exercise: The Movie Database

We enriched a `Person` from an external GraphQL API. Now enrich a `Movie` from an external *REST* API, [The Movie Database (TMDB)](https://www.themoviedb.org/). You will add two capabilities:

1. `tmdbSearch(title: String!): [TmdbResult!]!`, a query that searches TMDB's catalogue.
2. `Movie.communityRating`, a field carrying TMDB's live vote average, resolved with `@BatchMapping` so a list of movies makes one batched fetch rather than one call per movie.

The `TmdbService` that talks to TMDB already exists in the starter (it handles the HTTP, the API key, the caching, and the batching). Your job is to expose it through the schema, exactly as we exposed the countries API.

Its rating lookup follows the **cache-aside pattern** with Caffeine: it checks the cache first, fetches only the cache misses from TMDB, and stores the fresh results, so repeated ratings never re-hit the API and risk its rate limit.

### Getting a TMDB API key

Unlike the countries API, TMDB requires authentication. A key is free:

1. Create an account at [themoviedb.org](https://www.themoviedb.org/) and verify your email.
2. Go to **Settings → API** (`https://www.themoviedb.org/settings/api`) and request a developer key. Accept the terms; approval is instant.
3. On that page, copy the **API Read Access Token** (the long "v4" token, not the short v3 key). The service authenticates with `Authorization: Bearer <token>`.

Provide it to the app through the `TMDB_API_KEY` environment variable. The configuration in `application.yaml` already reads it:

```yaml
tmdb:
  api-key: ${TMDB_API_KEY:}
```

Start the backend with the variable set, for example:

```bash
TMDB_API_KEY='your-token-here' ./mvnw spring-boot:run
```

> Do not put the token in any file you commit. If `TMDB_API_KEY` is unset, the app still runs: `TmdbService` detects the missing key and returns empty results, so the rest of the API is unaffected. This is why TMDB is an exercise and not part of the core class.

### What to build

The schema additions, and the two thin controllers that delegate to `TmdbService`. Try it before opening the solution.

<details>
<summary>Solution</summary>

**Schema** (`src/main/resources/graphql/schema.graphqls`):

```graphql
type Query {
    # ...existing queries...
    """Search TMDB (The Movie Database) for movies by title"""
    tmdbSearch(title: String!): [TmdbResult!]!
}

type Movie {
    # ...existing fields...
    """Live community rating from TMDB; null when the movie has no tmdbId or TMDB is unavailable"""
    communityRating: CommunityRating
}

"""A movie search result from TMDB (The Movie Database)"""
type TmdbResult {
    tmdbId: Int!
    title: String!
    releaseYear: Int
    overview: String
    posterUrl: String
    "TMDB community vote average on a 0-10 scale"
    rating: Float
}

type CommunityRating {
    "Vote average on a 0-10 scale"
    voteAverage: Float!
    voteCount: Int!
}
```

**Search query** (`src/main/java/com/graphqlguy/moviedb/tmdb/TmdbController.java`, new):

```java
package com.graphqlguy.moviedb.tmdb;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TmdbController {

    private final TmdbService tmdbService;

    @QueryMapping
    List<TmdbResult> tmdbSearch(@Argument String title) {
        return tmdbService.search(title);
    }
}
```

**Community rating** (`src/main/java/com/graphqlguy/moviedb/tmdb/CommunityRatingController.java`, new). This is a `@BatchMapping`: Spring collects all the movies in the current selection and hands them to us as a list, so we make one call to `TmdbService.fetchMovieRatings` for the whole page instead of one per movie. The return is a `Map<Movie, CommunityRating>`; Spring matches each movie to its rating. Movies without a `tmdbId` are simply left out of the map and resolve to `null`.

```java
package com.graphqlguy.moviedb.tmdb;

import com.graphqlguy.moviedb.movie.Movie;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CommunityRatingController {

    private final TmdbService tmdbService;

    @BatchMapping(typeName = "Movie")
    Map<Movie, CommunityRating> communityRating(List<Movie> movies) {

        List<Movie> moviesWithTmdb = movies.stream()
                .filter(m -> m.getTmdbId() != null)
                .toList();

        if (moviesWithTmdb.isEmpty()) return Collections.emptyMap();

        List<Integer> tmdbIds = moviesWithTmdb.stream().map(Movie::getTmdbId).toList();
        Map<Integer, CommunityRating> ratings = tmdbService.fetchMovieRatings(tmdbIds);

        Map<Movie, CommunityRating> result = new HashMap<>();
        for (Movie movie : moviesWithTmdb) {
            CommunityRating rating = ratings.get(movie.getTmdbId());
            if (rating != null) {
                result.put(movie, rating);
            }
        }
        return result;
    }
}
```

With `TMDB_API_KEY` set, restart and try both:

```graphql
query {
  tmdbSearch(title: "godfather") { tmdbId title releaseYear rating }
  movies(size: 3) {
    content { title communityRating { voteAverage voteCount } }
  }
}
```

`tmdbSearch` returns live results from TMDB's catalogue, and every movie with a `tmdbId` comes back with its current community rating, all fetched in a single batched round of calls.

</details>
