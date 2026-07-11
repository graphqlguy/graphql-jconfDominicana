package com.graphqlguy.moviedb.config;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestContextInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String requestId = UUID.randomUUID().toString();
        request.configureExecutionInput((executionInput, builder) ->
                builder.graphQLContext(ctx -> ctx.put("requestId", requestId)).build());

        return chain.next(request).doOnNext(response ->
                response.getResponseHeaders().add("X-Request-Id", requestId));
    }
}
