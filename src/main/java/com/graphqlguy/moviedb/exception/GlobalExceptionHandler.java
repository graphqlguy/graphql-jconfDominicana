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
    public GraphQLError handleEntityNotFoundException(final EntityNotFoundException enfe, DataFetchingEnvironment environment) {
        return GraphqlErrorBuilder.newError(environment)
                .message(enfe.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of("entityType", enfe.getEntityType()))
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidInputException(final InvalidInputException iie, DataFetchingEnvironment environment) {
        return GraphqlErrorBuilder.newError(environment)
                .message(iie.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("field", iie.getField()))
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
    public GraphQLError handleDuplicateReview(DuplicateReviewException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConstraintViolation(ConstraintViolationException ex,
                                                  DataFetchingEnvironment env) {
        ConstraintViolation<?> first = ex.getConstraintViolations().iterator().next();
        String field = first.getPropertyPath().toString();
        return GraphqlErrorBuilder.newError(env)
                .message(field + " " + first.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("field", field))
                .build();
    }

    // Safety net for DB constraint violations (FKs, unique constraints, column
    // limits): without this they fall through to handleUnhandled and surface as
    // opaque 500s. Note: this is Spring's DataAccessException, not the jakarta
    // bean-validation ConstraintViolationException handled above.
    @GraphQlExceptionHandler
    public GraphQLError handleDataIntegrityViolation(final DataIntegrityViolationException ex,
                                                     DataFetchingEnvironment env) {
        log.warn("Data integrity violation at path={}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                .message("The request conflicts with existing data and could not be completed")
                .errorType(ErrorType.BAD_REQUEST)
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnhandled(final Exception e, DataFetchingEnvironment environment) {

        String reference = UUID.randomUUID().toString();
        log.error("Unhandled exception, reference={}, path={}", reference, environment.getExecutionStepInfo().getPath(), e);

        return GraphqlErrorBuilder.newError(environment)
                .message("An unexpected error occurred while processing the request. Reference: " + reference)
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of("reference", reference))
                .build();
    }
}
