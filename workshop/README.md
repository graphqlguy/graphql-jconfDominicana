# Workshop Guide

Welcome! This folder contains the step-by-step instructions we follow during the workshop. Each class is a separate markdown file; most end with exercises, with solutions or hints folded below each one (try first).

Before you start, make sure the project runs on your machine: see the [project README](../README.md) for prerequisites, seed users, and the optional TMDB API key.

## How this works

The repository you cloned is the **starting point**: a complete Spring Boot application (JPA entities, repositories, services, seed data, and a React frontend) with **no GraphQL in it at all**. We add the GraphQL API live, step by step. The finished application lives on the `completed` branch if you want to peek ahead.

The frontend is schema-aware: it asks the API what exists (via GraphQL introspection) and only uses what it finds. Every feature you build makes another part of the UI light up. Change the schema, restart the backend, refresh the browser.

## Classes

1. [Your first GraphQL service](01-your-first-graphql-service.md) - dependency, GraphiQL, your first schema, queries with arguments, pagination and filtering, deprecation
2. [Relationships and nested resolution](02-relationships-and-nested-resolution.md) - exposing entity relationships, default field resolution, your first `@SchemaMapping` resolver
3. [Mutations](03-mutations.md) - the Mutation root type, response types, structured errors, input types, partial updates
4. [Security](04-security.md) - Spring Security, JWT authentication, role-based authorization
5. [Error handling](05-error-handling.md) - the errors array, exception handlers, errors-as-data, partial responses
6. [Input validation](06-validation.md) - schema-directive validation, wiring the extended-validation library, Bean Validation
7. [Search and unions](07-search-and-unions.md) - typed search queries, union types, inline fragments, union vs interface
8. [Custom scalars and external APIs](08-custom-scalars-and-external-apis.md) - the extended-scalars library, CountryCode, calling an external GraphQL API, caching and graceful failure
9. [TV shows, interfaces, and a watch list](09-tv-shows-interfaces-and-watch-list.md) - exposing TV shows, interface types, `@oneOf` input types, a per-user authenticated feature
10. [Reviews, authorization, and subscriptions](10-reviews-authorization-and-subscriptions.md) - the DateTime scalar, object-level and field-level authorization, real-time updates over WebSocket
11. [Production strengthening](11-production-strengthening.md) - custom instrumentation, query-depth limiting, slow-resolver logging, request correlation, CORS
12. [Testing](12-testing.md) - the test pyramid, HttpGraphQlTester integration tests, authenticated mutations, subscriptions, the @GraphQlTest slice
