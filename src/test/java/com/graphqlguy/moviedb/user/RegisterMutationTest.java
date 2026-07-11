package com.graphqlguy.moviedb.user;

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
class RegisterMutationTest {

    private static final String REGISTER = """
            mutation Register($input: RegisterInput!) {
              register(input: $input) { token user { username email role } }
            }""";

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Test
    void register_createsUserAndReturnsToken() {
        graphQlTester.document(REGISTER)
                .variable("input", Map.of("username", "newuser", "email", "new@example.com", "password", "secret123"))
                .execute()
                .path("register.token").hasValue()
                .path("register.user.username").entity(String.class).isEqualTo("newuser")
                .path("register.user.role").entity(String.class).isEqualTo("USER");
    }

    @Test
    void register_tokenWorksForAuthenticatedMutations() {
        String token = graphQlTester.document(REGISTER)
                .variable("input", Map.of("username", "reviewer2", "email", "reviewer2@example.com", "password", "secret123"))
                .execute().path("register.token").entity(String.class).get();

        graphQlTester.mutate().headers(headers -> headers.setBearerAuth(token)).build()
                .document("""
                        mutation { createReview(input: {subject: {movieId: "3"}, score: 7}) { id user { username } } }
                        """)
                .execute()
                .path("createReview.user.username").entity(String.class).isEqualTo("reviewer2");
    }

    @Test
    void register_duplicateUsername_isRejected() {
        graphQlTester.document(REGISTER)
                .variable("input", Map.of("username", "admin", "email", "other@example.com", "password", "secret123"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).containsIgnoringCase("already taken")));
    }

    @Test
    void register_shortPassword_isRejectedBySchemaValidation() {
        graphQlTester.document(REGISTER)
                .variable("input", Map.of("username", "shorty", "email", "shorty@example.com", "password", "abc"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("password")));
    }

    @Test
    void register_invalidEmail_isRejectedBySchemaValidation() {
        graphQlTester.document(REGISTER)
                .variable("input", Map.of("username", "bademail", "email", "not-an-email", "password", "secret123"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("email")));
    }
}
