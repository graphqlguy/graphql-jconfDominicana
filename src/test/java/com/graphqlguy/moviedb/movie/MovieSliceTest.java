package com.graphqlguy.moviedb.movie;

import com.graphqlguy.moviedb.config.GraphQLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

// A slice test. @GraphQlTest loads only the GraphQL layer (the schema, the named
// @Controller, converters, exception handlers) and nothing else: no database, no
// security, no other controllers. The service is mocked, so this is dramatically
// faster than a full @SpringBootTest and still proves the schema wiring and the
// resolver are correct. GraphQLConfig is imported because the schema declares custom
// scalars (CountryCode, DateTime) that must be wired for it to load at all. The
// exclude filter keeps app-wide GraphQL Instrumentation beans out of this minimal
// slice; the pattern simply matches nothing on branches that have no such beans.
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
                .path("movie.title").entity(String.class).isEqualTo("Inception")
                .path("movie.releaseYear").entity(Integer.class).isEqualTo(2010);
    }
}
