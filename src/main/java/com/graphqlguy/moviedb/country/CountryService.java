package com.graphqlguy.moviedb.country;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CountryService {

    private static final String COUNTRY_QUERY = """
        query Country($code: ID!) {
          country(code: $code) {
            code
            name
            emoji
            capital
            currency
          }
        }
        """;

    // Without timeouts the JDK client waits forever; a hung external call would block
    // every request resolving the same country code behind the cache's per-key lock.
    private final HttpSyncGraphQlClient graphQlClient = HttpSyncGraphQlClient.create(
            RestClient.builder()
                    .baseUrl("https://countries.trevorblades.com/")
                    .requestFactory(timeoutRequestFactory())
                    .build());

    private final Cache<String, Country> countryCache;

    public Country findByCode(String code) {
        return countryCache.get(code, this::fetchByCode);
    }

    private static JdkClientHttpRequestFactory timeoutRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    private Country fetchByCode(String code) {
        return graphQlClient.document(COUNTRY_QUERY)
                .variable("code", code)
                .retrieveSync("country")
                .toEntity(Country.class);
    }
}