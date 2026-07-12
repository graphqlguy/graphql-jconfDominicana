package com.graphqlguy.moviedb.person;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @QueryMapping
    Person person(@Argument Long id) {
        return personService.findById(id);
    }

    @QueryMapping
    PersonPage people(@Argument Integer page, @Argument Integer size) {
        return personService.findAll(page != null ? page : 0, size != null ? size : 20);
    }
}
