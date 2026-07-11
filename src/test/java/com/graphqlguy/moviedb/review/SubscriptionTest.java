package com.graphqlguy.moviedb.review;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class SubscriptionTest {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Autowired
    ExecutionGraphQlService graphQlService;

    @Test
    void reviewAdded_pushesNotificationWhenReviewIsCreated() throws Exception {
        GraphQlTester subscriptionTester = ExecutionGraphQlServiceTester.create(graphQlService);
        Flux<GraphQlTester.Response> notifications = subscriptionTester.document("""
                        subscription { reviewAdded { title movieId review { score user { username } } } }
                        """)
                .executeSubscription()
                .toFlux();
        CompletableFuture<GraphQlTester.Response> firstNotification = notifications.next().toFuture();

        String token = graphQlTester.document("""
                        mutation { login(input: {username: "user", password: "user123"}) { token } }
                        """)
                .execute().path("login.token").entity(String.class).get();
        graphQlTester.mutate().headers(headers -> headers.setBearerAuth(token)).build()
                .document("""
                        mutation CreateReview($input: CreateReviewInput!) {
                          createReview(input: $input) { id }
                        }""")
                .variable("input", Map.of("subject", Map.of("movieId", "44"), "score", 8))
                .execute()
                .path("createReview.id").hasValue();

        GraphQlTester.Response notification = firstNotification.get(5, TimeUnit.SECONDS);
        notification.path("reviewAdded.movieId").entity(String.class).isEqualTo("44");
        notification.path("reviewAdded.review.score").entity(Integer.class).isEqualTo(8);
        notification.path("reviewAdded.review.user.username").entity(String.class).isEqualTo("user");
    }
}
