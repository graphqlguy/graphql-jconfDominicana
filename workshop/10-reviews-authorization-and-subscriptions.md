# Class 10: Reviews, Authorization, and Subscriptions

The catalog so far is read-mostly. Reviews make it social: a signed-in user rates and comments on a movie or show, and everyone sees the results, live. Building this is where several threads we have pulled separately come together, and it adds the last large pieces of the API:

- a second custom scalar at its use-site, `DateTime`,
- two kinds of authorization that go beyond the role checks of Class 4, and
- real-time updates pushed to clients over WebSocket.

By the end of this class, you will:

- Expose reviews, with a timestamp typed as `DateTime`
- Enforce **object-level** (ownership) and **field-level** authorization
- Push live updates to subscribers with a GraphQL **subscription**

As always, the domain layer is scaffolding: the `Review` entity and repository, `CreateReviewInput`, `ReviewSubjectInput` (the `@oneOf` input from Class 9), `DeleteReviewResponse`, the `MovieReviewCount` projection, and a basic `ReviewService` are already in the starter. This class adds the GraphQL layer, the authorization, and the subscription.

## 1. Reviews and the `DateTime` scalar

A review carries a timestamp. Rather than model it as a `String`, we use the `DateTime` scalar from the extended-scalars library, exactly as we did with `CountryCode` in Class 8. The library is already a dependency; we only register the scalar.

`src/main/java/com/graphqlguy/moviedb/config/GraphQLConfig.java`

```java
import graphql.scalars.ExtendedScalars;

// add DateTime alongside the CountryCode scalar from Class 8:
return wiringBuilder -> wiringBuilder
        .scalar(ExtendedScalars.DateTime)
        .scalar(ExtendedScalars.CountryCode)
        .directiveWiring(new ValidationSchemaWiring(validationRules));
```

Now the schema. A `Review` has a score, an optional comment, a `createdAt` typed as `DateTime`, and the user who wrote it. The mutations and the per-title fields come with it.

`src/main/resources/graphql/schema.graphqls`

```graphql
scalar DateTime

type Review {
    id: ID!
    "Rating from 1 to 10"
    score: Int!
    comment: String
    createdAt: DateTime!
    user: User!
}

type Movie {
    # ...existing fields...
    reviews: [Review!]!
    "Number of reviews, resolved as a grouped COUNT via @BatchMapping"
    reviewCount: Int!
}

type TvShow {
    # ...existing fields...
    reviews: [Review!]!
}

type Query {
    # ...unchanged...
}

type Mutation {
    # ...existing mutations...
    "Submit a review for a movie or TV show (one per user per title)"
    createReview(input: CreateReviewInput!): Review!
    "Delete a review (owner or admin only)"
    deleteReview(id: ID!): DeleteReviewResponse!
}
```

The `CreateReviewInput` and its `@oneOf` `ReviewSubjectInput` are scaffolding, and identical in shape to the watch-list input you built in Class 9:

```graphql
input CreateReviewInput {
    subject: ReviewSubjectInput!
    score: Int! @Range(min: 1, max: 10)
    comment: String @Size(max: 2000)
}

input ReviewSubjectInput @oneOf {
    movieId: ID
    tvShowId: ID
}
```

Note there is no re-teaching of `@oneOf` here. You learned it with the watch list; a review targets a movie or a show in exactly the same way, so you simply use it.

The controller exposes the mutation and resolves the per-title review lists with batch mappings, the same N+1-avoiding pattern from Class 2. `reviewCount` is resolved from a grouped `COUNT` query rather than by loading and counting reviews, which is what makes it cheap enough to show on every card of the landing page.

`src/main/java/com/graphqlguy/moviedb/review/ReviewController.java` (new)

```java
@Controller
@Validated
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final ReviewPublisher reviewPublisher;

    @MutationMapping
    Review createReview(@Argument @Valid CreateReviewInput input, Principal principal) {
        return reviewService.createReview(input, principal.getName());
    }

    @BatchMapping
    Map<Movie, List<Review>> reviews(List<Movie> movies) {
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        Map<Long, List<Review>> byMovieId = reviewRepository.findWithUserByMovieIdIn(movieIds)
                .stream().collect(Collectors.groupingBy(review -> review.getMovie().getId()));
        return movies.stream()
                .collect(Collectors.toMap(movie -> movie,
                        movie -> byMovieId.getOrDefault(movie.getId(), List.of())));
    }

    @BatchMapping(typeName = "Movie", field = "reviewCount")
    Map<Movie, Integer> reviewCount(List<Movie> movies) {
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        Map<Long, Long> counts = reviewRepository.countByMovieIdIn(movieIds)
                .stream().collect(Collectors.toMap(MovieReviewCount::movieId, MovieReviewCount::count));
        return movies.stream()
                .collect(Collectors.toMap(movie -> movie,
                        movie -> counts.getOrDefault(movie.getId(), 0L).intValue()));
    }

    @BatchMapping(typeName = "TvShow", field = "reviews")
    Map<TvShow, List<Review>> tvShowReviews(List<TvShow> tvShows) {
        List<Long> ids = tvShows.stream().map(TvShow::getId).toList();
        Map<Long, List<Review>> byShowId = reviewRepository.findWithUserByTvShowIdIn(ids)
                .stream().collect(Collectors.groupingBy(review -> review.getTvShow().getId()));
        return tvShows.stream()
                .collect(Collectors.toMap(tvShow -> tvShow,
                        tvShow -> byShowId.getOrDefault(tvShow.getId(), List.of())));
    }
}
```

Restart, sign in, and post a review:

```graphql
mutation {
  createReview(input: { subject: { movieId: 1 }, score: 9, comment: "Holds up." }) {
    id
    createdAt
    user { username }
  }
}
```

`createdAt` comes back as an ISO-8601 timestamp, serialized by the `DateTime` scalar. The frontend's review section and the review counts on the movie cards now light up.

## 2. Two kinds of authorization

Class 4 secured whole operations by role: only an admin may create a movie. Reviews need two finer-grained checks.

### Object-level: you may only delete your own review

Deleting a review is allowed if you are the review's author, or an admin. This is a decision about a *specific object*, not about a role alone, so the rule lives in the service where the object is loaded. We upgrade the scaffolding `ReviewService`.

`src/main/java/com/graphqlguy/moviedb/review/ReviewService.java`

```java
@Transactional
@PreAuthorize("isAuthenticated()")
public DeleteReviewResponse deleteReview(Long reviewId, String username) {
    Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));
    AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("Authenticated user has no matching record: " + username));

    boolean isOwner = review.getUser().getId().equals(user.getId());
    boolean isAdmin = user.getRole().name().equals("ADMIN");
    if (!isOwner && !isAdmin) {
        throw new AccessDeniedException("You can only delete your own reviews");
    }

    reviewRepository.delete(review);
    return new DeleteReviewResponse(true, reviewId);
}
```

The controller passes the caller's name down:

```java
@MutationMapping
DeleteReviewResponse deleteReview(@Argument Long id, Principal principal) {
    return reviewService.deleteReview(id, principal.getName());
}
```

`@PreAuthorize("isAuthenticated()")` rejects anonymous callers up front; the owner-or-admin check then decides the object-level question. A non-owner, non-admin attempt fails with a `FORBIDDEN` error.

### Field-level: an email is visible only to an admin or its owner

Some data should be hidden not per operation but per *field*. A reviewer's email address should be visible to an admin, and to the reviewer themselves, but to no one else, even though the review itself is public. We express that as a field resolver on `User.email`.

`src/main/java/com/graphqlguy/moviedb/user/UserController.java`

```java
@SchemaMapping(typeName = "User")
String email(AppUser user, Principal principal) {
    if (principal instanceof Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isSelf = auth.getName().equals(user.getUsername());
        if (isAdmin || isSelf) {
            return user.getEmail();
        }
    }
    return null;
}
```

The choice of a resolver over `@PreAuthorize` is deliberate. A method-security annotation on this field would raise an error for every non-admin, turning a normal page of reviews into a wall of authorization failures. A resolver instead returns a quiet `null`: the field is simply absent for viewers who may not see it, and the surrounding query succeeds. `User.email` is already nullable in the schema, so nothing else changes.

Try it. Query a movie's reviews while signed in as `admin` and the emails are present; as a regular user, other people's emails come back `null` while your own is visible; signed out, all are `null`. In the frontend, review cards show the email only when the server returns it.

## 3. Subscriptions: live updates

A query pulls data once. A **subscription** is a long-lived stream: the client opens a connection and the server pushes a value every time something happens. Spring for GraphQL runs subscriptions over WebSocket, on the same `/graphql` path.

We push a notification whenever a review is created. First, an in-memory publisher, a small bridge between the mutation that creates a review and the stream that subscribers read.

`src/main/java/com/graphqlguy/moviedb/review/ReviewPublisher.java` (new)

```java
@Component
public class ReviewPublisher {

    private final Sinks.Many<ReviewNotification> sink = Sinks.many().multicast().directBestEffort();

    public void publish(ReviewNotification notification) {
        // Sinks reject concurrent emission, so two simultaneous review mutations would
        // drop one notification; spin until our turn. Zero-subscriber drops are fine.
        Sinks.EmitResult result = sink.tryEmitNext(notification);
        while (result == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            Thread.onSpinWait();
            result = sink.tryEmitNext(notification);
        }
    }

    public Flux<ReviewNotification> flux() {
        return sink.asFlux();
    }
}
```

The `ReviewNotification` (scaffolding) carries the new review plus enough context to filter and display it:

```java
public record ReviewNotification(Review review, Long movieId, Long tvShowId, String title) {
    static ReviewNotification of(Review review) {
        if (review.getMovie() != null) {
            return new ReviewNotification(review, review.getMovie().getId(), null, review.getMovie().getTitle());
        }
        return new ReviewNotification(review, null, review.getTvShow().getId(), review.getTvShow().getTitle());
    }
}
```

`createReview` publishes after it saves. Add one line to the service:

```java
Review saved = reviewRepository.save(review.build());
reviewPublisher.publish(ReviewNotification.of(saved));
return saved;
```

The subscription itself is a resolver that returns a `Flux`. It is annotated `@SubscriptionMapping` and lives on the `ReviewController`. The optional `movieId` argument lets a client subscribe only to reviews of the movie it is currently viewing.

```java
@SubscriptionMapping
Flux<ReviewNotification> reviewAdded(@Argument Long movieId) {
    return reviewPublisher.flux()
            .filter(notification -> movieId == null || movieId.equals(notification.movieId()));
}
```

Finally the schema:

```graphql
type Subscription {
    "Live notifications when reviews are posted, pushed over WebSocket. Optionally filter by movieId."
    reviewAdded(movieId: ID): ReviewNotification!
}

"""Notification pushed to subscribers when a review is created"""
type ReviewNotification {
    review: Review!
    movieId: ID
    tvShowId: ID
    title: String!
}
```

The WebSocket endpoint is already configured in `application.yaml` (`spring.graphql.websocket.path: /graphql`). Restart, and try it with two GraphiQL tabs: in one, run the subscription and leave it open:

```graphql
subscription {
  reviewAdded {
    title
    review { score comment user { username } }
  }
}
```

In the other, post a review with `createReview`. The subscription tab receives the notification immediately, with no polling. The frontend does the same thing on a movie page: open a movie, and when anyone reviews it, a live toast appears.

## Recap

- A second use-site scalar, `DateTime`, registered exactly like `CountryCode`.
- **Object-level authorization** lives where the object is loaded: the owner-or-admin check on delete is a decision about a specific review, not just a role.
- **Field-level authorization** is a resolver that returns `null` for viewers who may not see a field, keeping the rest of the response intact, which is why it beats `@PreAuthorize` for hiding data.
- A **subscription** is a server-to-client stream over WebSocket; a publisher bridges the mutation that produces events to the `Flux` that subscribers consume.
- `@oneOf` and batch mappings returned here as tools, not lessons, which is the point of the ordering.

---

## Exercise: notify TV-show reviewers too

The `reviewAdded` subscription filters by `movieId`, so a client watching a TV show cannot subscribe to just that show's reviews. Extend the subscription with an optional `tvShowId` argument so a TV-show page can subscribe the same way a movie page does.

<details>
<summary>Solution</summary>

**Schema:**

```graphql
type Subscription {
    reviewAdded(movieId: ID, tvShowId: ID): ReviewNotification!
}
```

**Resolver** (`ReviewController`):

```java
@SubscriptionMapping
Flux<ReviewNotification> reviewAdded(@Argument Long movieId, @Argument Long tvShowId) {
    return reviewPublisher.flux()
            .filter(n -> movieId == null || movieId.equals(n.movieId()))
            .filter(n -> tvShowId == null || tvShowId.equals(n.tvShowId()));
}
```

With no arguments the client receives every review; with either id it receives only that title's. The `ReviewNotification` already carries both `movieId` and `tvShowId`, so no other change is needed.

</details>
</content>
