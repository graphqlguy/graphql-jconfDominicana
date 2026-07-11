package com.graphqlguy.moviedb.tmdb;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    private final TmdbProperties tmdbProperties;
    private final Cache<Integer, CommunityRating> ratingCache;
    // Without timeouts the JDK client waits forever; a hung TMDB call would block
    // fetchMovieRatings (executor.close() waits for all tasks) and the whole query with it.
    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

    public TmdbService(final TmdbProperties tmdbProperties, final Cache<Integer, CommunityRating> ratingCache) {
        this.tmdbProperties = tmdbProperties;
        this.ratingCache = ratingCache;
    }

    private static JdkClientHttpRequestFactory timeoutRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    public List<TmdbResult> search(String title) {
        if (!tmdbProperties.hasApiKey()) {
            log.warn("TMDB_API_KEY is not configured. TMDB search unavailable.");
            return Collections.emptyList();
        }
        try {
            TmdbSearchResponse response = restClient.get()
                    .uri(tmdbProperties.baseUrl() + "/search/movie?query={query}&language=en-US&page=1", title)
                    .header("Authorization", "Bearer " + tmdbProperties.apiKey())
                    .retrieve()
                    .body(TmdbSearchResponse.class);

            if (response == null || response.results() == null)
                return Collections.emptyList();

            return response.results().stream().limit(10).map(r -> new TmdbResult(
                    r.id(), r.title() != null ? r.title() : "",
                    parseYear(r.releaseDate()), r.overview(),
                    r.posterPath() != null ? tmdbProperties.imageBaseUrl() + r.posterPath() : null,
                    r.voteAverage() != null ? Math.round(r.voteAverage() * 10.0) / 10.0 : null
            )).toList();
        } catch (Exception e) {
            log.error("TMDB search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<Integer, CommunityRating> fetchMovieRatings(List<Integer> tmdbIds) {
        if (!tmdbProperties.hasApiKey()) {
            log.warn("TMDB_API_KEY not configured. Community ratings unavailable.");
            return Collections.emptyMap();
        }

        Map<Integer, CommunityRating> results = new HashMap<>();
        List<Integer> cacheMisses = new ArrayList<>();

        // Step 1: Check cache for each ID
        for (Integer tmdbId : tmdbIds) {
            CommunityRating cached = ratingCache.getIfPresent(tmdbId);
            if (cached != null) {
                results.put(tmdbId, cached);
            } else {
                cacheMisses.add(tmdbId);
            }
        }

        // Step 2: Fetch only cache misses, in parallel using virtual threads
        if (!cacheMisses.isEmpty()) {
            log.info("TMDB cache miss for {} IDs, fetching from API", cacheMisses.size());
            Map<Integer, CommunityRating> fetched = new ConcurrentHashMap<>();
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                cacheMisses.forEach(tmdbId -> executor.submit(() -> {
                    try {
                        TmdbMovieDetails details = restClient.get()
                                .uri(tmdbProperties.baseUrl() + "/movie/{id}?language=en-US", tmdbId)
                                .header("Authorization", "Bearer " + tmdbProperties.apiKey())
                                .retrieve()
                                .body(TmdbMovieDetails.class);
                        if (details != null) {
                            CommunityRating rating = new CommunityRating(
                                    Math.round(details.voteAverage() * 10.0) / 10.0,
                                    details.voteCount());
                            fetched.put(tmdbId, rating);
                            ratingCache.put(tmdbId, rating);  // Step 3: Cache the result
                        }
                    } catch (Exception e) {
                        log.debug("Failed to fetch TMDB details for tmdbId={}: {}",
                                tmdbId, e.getMessage());
                    }
                }));
            }
            results.putAll(fetched);
        } else {
            log.info("All {} TMDB ratings served from cache", results.size());
        }

        return results;
    }

    private Integer parseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}