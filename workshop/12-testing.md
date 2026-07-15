# Class 12: Testing

The API is built and hardened. This final class is about proving it works, and keeping it working. GraphQL rewards a particular testing style. In a REST service the controllers are thin and a unit test of the service covers most of the risk. In GraphQL the interesting behaviour lives in the machinery *around* your resolvers: the query is parsed, validated against the schema, fields are resolved and batched, security is applied, and errors are shaped into the response. A unit test of one resolver sees none of that. So we lean on **integration tests** that drive the real GraphQL endpoint, and reach for the faster slice test where it pays off.

By the end of this class, you will:

- Test queries, mutations, and subscriptions through `HttpGraphQlTester`
- Authenticate a test by minting a real JWT
- Assert on both data and the `errors` array
- Use the `@GraphQlTest` slice for fast, mocked tests

## 1. The test pyramid for GraphQL

| Level | What it covers | What it misses |
| --- | --- | --- |
| Unit | one method's logic | schema wiring, security, batching, error shaping |
| Slice (`@GraphQlTest`) | schema + your controllers, fast, service mocked | real persistence, real security |
| Integration (`@SpringBootTest` + `HttpGraphQlTester`) | the whole pipeline end to end | nothing (but slower) |

Most of your tests should be integration tests, because that is where GraphQL's risk concentrates. Slice tests are the fast layer for resolver logic that does not need a database.

## 2. The dependency

Testing GraphQL needs one starter, which brings `HttpGraphQlTester`, `GraphQlTester`, and the `@GraphQlTest` slice annotation.

`pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 3. Integration tests: `HttpGraphQlTester`

An integration test boots the whole application and sends real GraphQL requests to it. Three annotations set it up; `HttpGraphQlTester` is then injected for you.

`src/test/java/com/graphqlguy/moviedb/person/PersonQueryTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class PersonQueryTest {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Test
    void countryCode_isExposedAsExtendedScalar() {
        graphQlTester.document("""
                        { searchPeople(name: "Christopher Nolan") { countryCode } }
                        """)
                .execute()
                .path("searchPeople[0].countryCode").entity(String.class).isEqualTo("GB");
    }
}
```

The shape is always the same: `document(...)` sets the query, `execute()` runs it, and `path(...)` navigates the response by JSONPath. From a path you `.entity(Type.class)` to deserialize and assert with `.isEqualTo(...)` or `.satisfies(...)`, or `.entityList(Type.class)` for a list. The `[0]` and `[*]` JSONPath forms index into arrays. This one test exercises the schema, the resolver, and the `CountryCode` scalar's serialization together.

## 4. Mutations with authentication

Most mutations require a signed-in user. Rather than mock security, we get a real token the way a client would, by calling `login`, then build a tester that carries it. A small helper keeps the tests readable.

`src/test/java/com/graphqlguy/moviedb/movie/MovieMutationTest.java`

```java
private HttpGraphQlTester loggedInAs(String username, String password) {
    String token = graphQlTester.document("""
                    mutation { login(input: {username: "%s", password: "%s"}) { token } }
                    """.formatted(username, password))
            .execute().path("login.token").entity(String.class).get();
    return graphQlTester.mutate()
            .headers(headers -> headers.setBearerAuth(token))
            .build();
}

@Test
void createMovie_asAdmin_createsMovie() {
    loggedInAs("admin", "admin123")
            .document(CREATE_MOVIE)
            .variable("input", Map.of("title", "Test Movie", "releaseYear", 2020, "genre", "DRAMA", "rating", 7.5))
            .execute()
            .path("createMovie.id").hasValue()
            .path("createMovie.title").entity(String.class).isEqualTo("Test Movie");
}
```

Two mechanics here. `.get()` extracts a value from one response to use in the next request, which is how the token flows from the login into the header. And `graphQlTester.mutate()` produces a *new* tester that inherits everything from the original but adds the `Authorization` header, so the base tester stays unauthenticated for the tests that need it that way. `.variable(...)` binds a GraphQL variable, and `.hasValue()` asserts a field is present and non-null without pinning its exact value.

## 5. Asserting on errors

Half of an API's contract is how it fails. `HttpGraphQlTester` reads the `errors` array with `.errors().satisfy(...)`. This is how we prove that schema-directive validation actually rejects bad input:

```java
@Test
void createMovie_releaseYearOutOfRange_isRejectedBySchemaValidation() {
    loggedInAs("admin", "admin123")
            .document(CREATE_MOVIE)
            .variable("input", Map.of("title", "Ancient Movie", "releaseYear", 1700, "genre", "DRAMA"))
            .execute()
            .errors().satisfy(errors -> assertThat(errors)
                    .anySatisfy(error -> assertThat(error.getMessage()).contains("releaseYear")));
}
```

> [!TIP]
> if a `.path(...)` assertion fails with a confusing message, the request probably returned an error you are not expecting. Add `.errors().verify()` (which asserts there are none) before the path, and the real GraphQL error surfaces.

## 6. Testing a subscription

A subscription is a stream, so it is tested differently: subscribe first, capture the first emission as a future, then trigger the event and assert on what arrives. `HttpGraphQlTester` speaks HTTP, so for the subscription we use an `ExecutionGraphQlServiceTester`, which runs against the `ExecutionGraphQlService` directly.

`src/test/java/com/graphqlguy/moviedb/review/SubscriptionTest.java`

```java
@Test
void reviewAdded_pushesNotificationWhenReviewIsCreated() throws Exception {
    GraphQlTester subscriptionTester = ExecutionGraphQlServiceTester.create(graphQlService);
    Flux<GraphQlTester.Response> notifications = subscriptionTester.document("""
                    subscription { reviewAdded { title movieId review { score user { username } } } }
                    """)
            .executeSubscription()
            .toFlux();
    CompletableFuture<GraphQlTester.Response> firstNotification = notifications.next().toFuture();

    // Trigger the event: an authenticated user posts a review.
    String token = graphQlTester.document("""
                    mutation { login(input: {username: "user", password: "user123"}) { token } }
                    """).execute().path("login.token").entity(String.class).get();
    graphQlTester.mutate().headers(h -> h.setBearerAuth(token)).build()
            .document("mutation($i: CreateReviewInput!){ createReview(input:$i){ id } }")
            .variable("i", Map.of("subject", Map.of("movieId", "44"), "score", 8))
            .execute().path("createReview.id").hasValue();

    GraphQlTester.Response notification = firstNotification.get(5, TimeUnit.SECONDS);
    notification.path("reviewAdded.review.score").entity(Integer.class).isEqualTo(8);
    notification.path("reviewAdded.review.user.username").entity(String.class).isEqualTo("user");
}
```

Subscribing before triggering is essential: the publisher drops events with no subscriber, so the order matters. The future with a timeout keeps the test from hanging if the notification never comes.

## 7. Faster feedback: the `@GraphQlTest` slice

Booting the whole application for every test is thorough but slow. `@GraphQlTest` loads only the GraphQL layer, the schema, the named `@Controller`, converters, exception handlers, and nothing else. There is no database and no security, so services are mocked with `@MockitoBean`.

`src/test/java/com/graphqlguy/moviedb/movie/MovieSliceTest.java`

```java
@GraphQlTest(value = MovieController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.graphqlguy\\.moviedb\\.instrumentation\\..*"))
@Import(GraphQLConfig.class)
class MovieSliceTest {

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    MovieService movieService;

    @Test
    void movie_resolvesFieldsFromTheService() {
        Movie movie = Movie.builder().id(1L).title("Inception").releaseYear(2010).build();
        when(movieService.findById(1L)).thenReturn(movie);

        graphQlTester.document("{ movie(id: 1) { title releaseYear } }")
                .execute()
                .path("movie.title").entity(String.class).isEqualTo("Inception");
    }
}
```

Two details this slice needs. `@Import(GraphQLConfig.class)` brings the custom-scalar wiring, without which the schema (which declares `DateTime` and `CountryCode`) will not load. And the `excludeFilter` keeps the app-wide instrumentation beans out of this minimal slice, since they expect configuration this cut-down context does not provide. The test proves the schema wiring and the resolver are correct in a fraction of the time an integration test takes.

## 8. Running the tests

```bash
./mvnw test                       # everything
./mvnw test -Dtest=MovieSliceTest # one class
```

One caveat worth knowing: the integration tests share a database, seeded once by `DataInitializer`. A mutation test that creates or deletes data can influence a later test, so keep tests independent of each other's ordering, and clean up (or assert against seed data you do not mutate).

## Recap

- GraphQL's risk lives around the resolver (schema, security, batching, errors), so integration tests through `HttpGraphQlTester` carry most of the load.
- `document().execute().path().entity()` is the whole assertion vocabulary; `.errors().satisfy()` covers the failure half of the contract.
- Authenticate by minting a real JWT and building a tester with `mutate().headers(...)`.
- Subscriptions are tested by subscribing first, then triggering the event.
- `@GraphQlTest` with `@MockitoBean` is the fast slice for resolver logic that needs neither database nor security.

---

## Exercise: prove authorization, not just happy paths

The suite tests that an admin can create a movie. Add the tests that make authorization real:

1. Creating a movie **without** a token is rejected.
2. Creating a movie as a **non-admin** user is rejected.
3. A user can delete **their own** review but not someone else's.

<details>
<summary>Hint</summary>

For the negative cases, assert on `.errors()`: an unauthenticated mutation returns an error classified `UNAUTHORIZED`, an unauthorized one `FORBIDDEN`. Use the base `graphQlTester` (no token) for case 1 and `loggedInAs("user", "user123")` for cases 2 and 3. For the ownership case, create a review as one user and attempt to delete it as another — and note that the only other seeded account is the admin, who *is* allowed, so `register` a fresh user for the foreign-delete attempt.

</details>
