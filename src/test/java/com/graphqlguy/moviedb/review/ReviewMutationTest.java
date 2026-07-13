package com.graphqlguy.moviedb.review;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class ReviewMutationTest {

    private static final String CREATE_REVIEW = """
            mutation CreateReview($input: CreateReviewInput!) {
              createReview(input: $input) { id score comment user { username } }
            }""";

    @Autowired
    HttpGraphQlTester graphQlTester;

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
    void createReview_forMovie_succeeds() {
        loggedInAs("user", "user123")
                .document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("movieId", "1"), "score", 9, "comment", "Classic."))
                .execute()
                .path("createReview.id").hasValue()
                .path("createReview.score").entity(Integer.class).isEqualTo(9);
    }

    @Test
    void createReview_forTvShow_succeedsAndAppearsOnShow() {
        loggedInAs("user", "user123")
                .document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("tvShowId", "1"), "score", 10, "comment", "Still funny."))
                .execute()
                .path("createReview.score").entity(Integer.class).isEqualTo(10);

        graphQlTester.document("{ tvShow(id: 1) { reviews { score comment user { username } } } }")
                .execute()
                .path("tvShow.reviews[0].score").entity(Integer.class).isEqualTo(10)
                .path("tvShow.reviews[0].user.username").entity(String.class).isEqualTo("user");
    }

    @Test
    void createReview_withBothTargets_isRejectedByOneOf() {
        loggedInAs("user", "user123")
                .document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("movieId", "1", "tvShowId", "1"), "score", 5))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).containsIgnoringCase("exactly one")));
    }

    @Test
    void createReview_withNoTarget_isRejectedByOneOf() {
        loggedInAs("user", "user123")
                .document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of(), "score", 5))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).containsIgnoringCase("exactly one")));
    }

    @Test
    void createReview_duplicateForSameShow_isRejected() {
        HttpGraphQlTester admin = loggedInAs("admin", "admin123");
        admin.document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("tvShowId", "2"), "score", 8))
                .execute()
                .path("createReview.id").hasValue();

        admin.document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("tvShowId", "2"), "score", 6))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).containsIgnoringCase("already reviewed")));
    }

    @Test
    void reviewCount_reflectsNumberOfReviews() {
        graphQlTester.document("{ movie(id: 45) { reviewCount } }")
                .execute()
                .path("movie.reviewCount").entity(Integer.class).isEqualTo(0);

        loggedInAs("user", "user123")
                .document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("movieId", "45"), "score", 7))
                .execute()
                .path("createReview.id").hasValue();

        graphQlTester.document("{ movie(id: 45) { reviewCount } }")
                .execute()
                .path("movie.reviewCount").entity(Integer.class).isEqualTo(1);
    }

    @Test
    void createReview_scoreOutOfRange_isRejectedBySchemaValidation() {
        loggedInAs("user", "user123")
                .document(CREATE_REVIEW)
                .variable("input", Map.of("subject", Map.of("movieId", "2"), "score", 11))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("score")));
    }
}
