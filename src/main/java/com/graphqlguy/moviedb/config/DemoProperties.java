package com.graphqlguy.moviedb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Teaching-only knobs for the demo app.
 *
 * @param latency artificial delay injected into resolvers to simulate a slow
 *                dependency (DB / external service). {@code 0} (the default) disables it.
 */
@ConfigurationProperties(prefix = "demo")
public record DemoProperties(@DefaultValue("0") Duration latency) {
}
