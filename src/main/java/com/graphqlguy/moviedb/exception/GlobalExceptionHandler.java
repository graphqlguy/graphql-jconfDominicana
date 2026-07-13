package com.graphqlguy.moviedb.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleEntityNotFound(final EntityNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of("entityType", ex.getEntityType()))
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidInput(final InvalidInputException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("field", ex.getField()))
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleDuplicateReview(DuplicateReviewException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleAccessDenied(AccessDeniedException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message("You are not authorized to perform this action")
                .errorType(ErrorType.FORBIDDEN)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConstraintViolation(ConstraintViolationException ex, DataFetchingEnvironment env) {
        ConstraintViolation<?> first = ex.getConstraintViolations().iterator().next();
        String field = first.getPropertyPath().toString();
        return GraphqlErrorBuilder.newError(env)
                .message(field + " " + first.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("field", field))
                .build();
    }

    // Safety net for database constraints (foreign keys, unique constraints);
    // this is Spring's DataAccessException, not the Jakarta validation one above.
    @GraphQlExceptionHandler
    public GraphQLError handleDataIntegrityViolation(final DataIntegrityViolationException ex, DataFetchingEnvironment env) {
        log.warn("Data integrity violation at path={}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                .message("The request conflicts with existing data and could not be completed")
                .errorType(ErrorType.BAD_REQUEST)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnhandled(final Exception ex, DataFetchingEnvironment env) {
        String reference = UUID.randomUUID().toString();
        log.error("Unhandled exception, reference={}, path={}", reference, env.getExecutionStepInfo().getPath(), ex);
        return GraphqlErrorBuilder.newError(env)
                .message("An unexpected error occurred while processing the request. Reference: " + reference)
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of("reference", reference))
                .build();
    }
}
