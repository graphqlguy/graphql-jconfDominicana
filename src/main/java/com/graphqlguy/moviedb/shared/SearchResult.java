package com.graphqlguy.moviedb.shared;

/**
 * Marker for types that belong to the SearchResult GraphQL union.
 * Not sealed: in a non-modular application Java requires permitted subtypes
 * to live in the same package as the sealed type, and Movie/Person live in
 * separate packages.
 */
public interface SearchResult {
}
