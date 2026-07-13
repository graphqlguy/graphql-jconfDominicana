package com.graphqlguy.moviedb.shared;

/**
 * The GraphQL Content interface: the fields a Movie and a TvShow have in common.
 * Declaring the accessors here makes the shared contract compiler-checked (both
 * entities already expose them via Lombok). Spring for GraphQL resolves the concrete
 * type, Movie or TvShow, by matching the class name to the schema type.
 */
public interface Content {

    String getTitle();

    Genre getGenre();

    Double getRating();

    String getPosterUrl();
}
