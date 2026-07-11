package com.graphqlguy.moviedb.movie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class MovieMutationTest {

    private static final String CREATE_MOVIE = """
            mutation CreateMovie($input: CreateMovieInput!) {
              createMovie(input: $input) { id title releaseYear rating }
            }""";

    private static final String UPDATE_MOVIE = """
            mutation UpdateMovie($input: UpdateMovieInput!) {
              updateMovie(input: $input) { id title releaseYear rating }
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
    void createMovie_asAdmin_createsMovie() {
        loggedInAs("admin", "admin123")
                .document(CREATE_MOVIE)
                .variable("input", Map.of("title", "Test Movie", "releaseYear", 2020, "genre", "DRAMA", "rating", 7.5))
                .execute()
                .path("createMovie.id").hasValue()
                .path("createMovie.title").entity(String.class).isEqualTo("Test Movie")
                .path("createMovie.rating").entity(Double.class).isEqualTo(7.5);
    }

    @Test
    void createMovie_releaseYearOutOfRange_isRejectedBySchemaValidation() {
        loggedInAs("admin", "admin123")
                .document(CREATE_MOVIE)
                .variable("input", Map.of("title", "Ancient Movie", "releaseYear", 1700, "genre", "DRAMA"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("releaseYear")));
    }

    @Test
    void createMovie_ratingOutOfRange_isRejectedBySchemaValidation() {
        loggedInAs("admin", "admin123")
                .document(CREATE_MOVIE)
                .variable("input", Map.of("title", "Overrated", "releaseYear", 2020, "genre", "DRAMA", "rating", 11.0))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("rating")));
    }

    @Test
    void createMovie_titleTooLong_isRejectedBySchemaValidation() {
        loggedInAs("admin", "admin123")
                .document(CREATE_MOVIE)
                .variable("input", Map.of("title", "X".repeat(201), "releaseYear", 2020, "genre", "DRAMA"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("title")));
    }

    @Test
    void updateMovie_partialUpdate_changesOnlyGivenFields() {
        HttpGraphQlTester admin = loggedInAs("admin", "admin123");
        String id = admin.document(CREATE_MOVIE)
                .variable("input", Map.of("title", "To Update", "releaseYear", 2001, "genre", "DRAMA", "rating", 5.0))
                .execute().path("createMovie.id").entity(String.class).get();

        admin.document(UPDATE_MOVIE)
                .variable("input", Map.of("id", id, "rating", 8.0))
                .execute()
                .path("updateMovie.title").entity(String.class).isEqualTo("To Update")
                .path("updateMovie.releaseYear").entity(Integer.class).isEqualTo(2001)
                .path("updateMovie.rating").entity(Double.class).isEqualTo(8.0);
    }

    @Test
    void createMovie_asRegularUser_isForbidden() {
        loggedInAs("user", "user123")
                .document(CREATE_MOVIE)
                .variable("input", Map.of("title", "Sneaky", "releaseYear", 2020, "genre", "DRAMA"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("not authorized")));
    }
}
