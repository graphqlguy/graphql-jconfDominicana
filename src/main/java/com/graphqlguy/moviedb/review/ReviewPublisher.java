package com.graphqlguy.moviedb.review;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * In-memory pub/sub bridge between review mutations and the reviewAdded
 * subscription. directBestEffort: events are simply dropped when nobody
 * is subscribed.
 */
@Component
public class ReviewPublisher {

    private final Sinks.Many<ReviewNotification> sink = Sinks.many().multicast().directBestEffort();

    public void publish(ReviewNotification notification) {
        // Sinks reject concurrent emission (FAIL_NON_SERIALIZED), so two simultaneous
        // review mutations would silently drop one notification; spin until our turn.
        // Other failures (e.g. FAIL_ZERO_SUBSCRIBER) remain intentional drops.
        Sinks.EmitResult result = sink.tryEmitNext(notification);
        while (result == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            Thread.onSpinWait();
            result = sink.tryEmitNext(notification);
        }
    }

    public Flux<ReviewNotification> flux() {
        return sink.asFlux();
    }
}
