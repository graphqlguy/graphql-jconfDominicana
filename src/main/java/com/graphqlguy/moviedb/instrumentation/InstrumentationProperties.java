package com.graphqlguy.moviedb.instrumentation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "graphql")
public record InstrumentationProperties(
        @DefaultValue("10") int maxQueryDepth,
        @DefaultValue("100") long slowResolverThresholdMs) {}
