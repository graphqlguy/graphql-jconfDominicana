package com.graphqlguy.moviedb.country;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @QueryMapping
    public Country country(@Argument String code) {
        return countryService.findByCode(code);
    }
}
