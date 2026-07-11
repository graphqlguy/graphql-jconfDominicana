package com.graphqlguy.moviedb.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(
        @DefaultValue("") String apiKey,
        String baseUrl,
        String imageBaseUrl) {

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}