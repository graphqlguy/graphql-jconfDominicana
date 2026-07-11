package com.graphqlguy.moviedb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Simulates a slow, blocking dependency (a database query or an external API call) by
 * sleeping for the configured {@code demo.latency}. On virtual threads this sleep unmounts
 * the carrier thread, so it faithfully models non-pinning blocking I/O.
 *
 * <p>Centralising the delay here means one property controls it globally, and the same seam
 * can later be backed by a real slow HTTP call (e.g. a WireMock stub) without touching callers.
 */
@Component
@RequiredArgsConstructor
public class LatencySimulator {

    private final DemoProperties demoProperties;

    /** Blocks the current thread for the configured latency; a zero/negative duration is a no-op. */
    public void pause() {
        Duration delay = demoProperties.latency();
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while simulating latency", e);
        }
    }
}
