package com.graphqlguy.moviedb.person;

public record DeletePersonResponse(boolean success, DeletePersonError error, Long deletedId) {
}
