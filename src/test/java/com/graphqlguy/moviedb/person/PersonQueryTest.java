package com.graphqlguy.moviedb.person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class PersonQueryTest {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Test
    void createdShows_returnsShowsThePersonCreated() {
        graphQlTester.document("""
                        { searchPeople(name: "David Crane") { name createdShows { title } } }
                        """)
                .execute()
                .path("searchPeople[0].name").entity(String.class).isEqualTo("David Crane")
                .path("searchPeople[0].createdShows[*].title").entityList(String.class)
                .satisfies(titles -> assertThat(titles).containsExactly("Friends"));
    }

    @Test
    void countryCode_isExposedAsExtendedScalar() {
        graphQlTester.document("""
                        { searchPeople(name: "Christopher Nolan") { countryCode } }
                        """)
                .execute()
                .path("searchPeople[0].countryCode").entity(String.class).isEqualTo("GB");
    }

    @Test
    void createPerson_withInvalidCountryCode_isRejectedByScalarCoercion() {
        String token = graphQlTester.document("""
                        mutation { login(input: {username: "admin", password: "admin123"}) { token } }
                        """)
                .execute().path("login.token").entity(String.class).get();

        graphQlTester.mutate().headers(headers -> headers.setBearerAuth(token)).build()
                .document("""
                        mutation CreatePerson($input: CreatePersonInput!) {
                          createPerson(input: $input) { id }
                        }""")
                .variable("input", java.util.Map.of("name", "Test", "countryCode", "XX"))
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(error -> assertThat(error.getMessage()).contains("XX")));
    }

    @Test
    void createdShows_isEmptyForNonCreators() {
        graphQlTester.document("""
                        { searchPeople(name: "Tom Hanks") { createdShows { title } } }
                        """)
                .execute()
                .path("searchPeople[0].createdShows").entityList(Object.class)
                .satisfies(shows -> assertThat(shows).isEmpty());
    }
}
