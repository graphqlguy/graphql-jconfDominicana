package com.graphqlguy.moviedb.instrumentation;

import graphql.execution.instrumentation.FieldFetchingInstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FieldTimingInstrumentation extends SimplePerformantInstrumentation {

    private final InstrumentationProperties properties;

    @Override
    public FieldFetchingInstrumentationContext beginFieldFetching(
            InstrumentationFieldFetchParameters parameters,
            InstrumentationState state) {
        long startNanos = System.nanoTime();
        String fieldPath = parameters.getEnvironment()
                .getExecutionStepInfo().getPath().toString();

        return new FieldFetchingInstrumentationContext() {
            @Override
            public void onDispatched() { }

            @Override
            public void onCompleted(Object result, Throwable error) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                if (durationMs >= properties.slowResolverThresholdMs()) {
                    log.warn("Slow resolver: {} took {}ms", fieldPath, durationMs);
                }
            }
        };
    }
}
