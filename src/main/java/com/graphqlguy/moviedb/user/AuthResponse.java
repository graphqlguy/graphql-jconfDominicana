package com.graphqlguy.moviedb.user;

public record AuthResponse(String token, AppUser user) {
}
