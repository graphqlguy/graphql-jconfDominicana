package com.graphqlguy.moviedb.person;

import com.graphqlguy.moviedb.country.Country;
import com.graphqlguy.moviedb.country.CountryService;
import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.movie.MovieCast;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final CountryService countryService;

    @QueryMapping
    Person person(@Argument Long id) {
        return personService.findById(id);
    }

    @QueryMapping
    PersonPage people(@Argument Integer page, @Argument Integer size) {
        return personService.findAll(page != null ? page : 0, size != null ? size : 20);
    }

    @QueryMapping
    List<Person> searchPeople(@Argument String name) {
        return personService.searchByName(name);
    }

    @SchemaMapping(typeName = "Person")
    Country country(Person person) {
        if (person.getCountryCode() == null) {
            return null;
        }
        try {
            return countryService.findByCode(person.getCountryCode());
        } catch (Exception e) {
            // The external countries API being down should not break person queries
            log.warn("Could not resolve country {} from external API: {}", person.getCountryCode(), e.getMessage());
            return null;
        }
    }

    @BatchMapping
    Map<Movie, List<Person>> directors(List<Movie> movies) {
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        Map<Long, List<Person>> directorsByMovieId = personService.findDirectorsByMovieIds(movieIds);
        return movies.stream()
                .collect(Collectors.toMap(movie -> movie,
                        movie -> directorsByMovieId.getOrDefault(movie.getId(), List.of())));
    }

    @BatchMapping
    Map<Person, List<Movie>> directedMovies(List<Person> people) {
        List<Long> personIds = people.stream().map(Person::getId).toList();
        Map<Long, List<Movie>> moviesByPersonId = personService.findDirectedMoviesByPersonIds(personIds);
        return people.stream()
                .collect(Collectors.toMap(person -> person,
                        person -> moviesByPersonId.getOrDefault(person.getId(), List.of())));
    }

    @BatchMapping
    Map<Movie, List<MovieCast>> cast(List<Movie> movies) {
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        Map<Long, List<MovieCast>> castByMovieId = personService.findCastByMovieIds(movieIds);
        return movies.stream()
                .collect(Collectors.toMap(movie -> movie,
                        movie -> castByMovieId.getOrDefault(movie.getId(), List.of())));
    }

    @BatchMapping
    Map<Person, List<MovieCast>> movieCastCredits(List<Person> people) {
        List<Long> personIds = people.stream().map(Person::getId).toList();
        Map<Long, List<MovieCast>> creditsByPersonId = personService.findMovieCastCreditsByPersonIds(personIds);
        return people.stream()
                .collect(Collectors.toMap(person -> person,
                        person -> creditsByPersonId.getOrDefault(person.getId(), List.of())));
    }


    @MutationMapping
    Person createPerson(@Argument @Valid CreatePersonInput input) {
        return personService.createPerson(input);
    }

    @MutationMapping
    Person updatePerson(@Argument UpdatePersonInput input) {
        return personService.updatePerson(input);
    }

    @MutationMapping
    DeletePersonResponse deletePerson(@Argument Long id, @Argument boolean force) {
        return personService.delete(id, force);
    }
}
