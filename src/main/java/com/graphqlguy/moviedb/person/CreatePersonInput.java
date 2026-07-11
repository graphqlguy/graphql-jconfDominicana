package com.graphqlguy.moviedb.person;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePersonInput(
        @NotBlank @Size(max = 100) String name,
        @Min(1850) Integer birthYear,
        @Size(min = 2, max = 2) String countryCode) {
}
