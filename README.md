# From Zero to Query: Build, Secure, and Deploy a Production-Ready GraphQL API in One Afternoon

Companion repository for the hands-on workshop at **[JConf Dominicana](https://jconfdominicana.org)**, presented by **Željko Kozina**.

> In this hands-on workshop, you'll go from an empty project to a deployed, production-grade GraphQL API - no prior GraphQL experience required. Basic Spring Boot knowledge is desired. Starting with schema design fundamentals, we'll incrementally build a fully functional API using Spring for GraphQL. Each module builds on the last: defining your schema and first resolvers; modelling relationships and solving the N+1 problem with DataLoaders; adding field-level authentication and input validation; and writing integration tests with GraphQL-specific assertions. By the end, you'll have a running service you built yourself, a clear understanding of GraphQL's core concepts, and - just as importantly - the judgment to know when GraphQL is the right choice and when it isn't. Bring your laptop and your curiosity.

This branch (**`completed`**) contains the finished application we build during the workshop: **MovieDB**, a Spring for GraphQL API with a React frontend.

## What We'll Cover

Topics include:

1. Your first Spring GraphQL service - project setup, schema-first development, your first query
2. Schema design & relationships - enums, JPA persistence, entity relationships
3. Nested resolution - junction entities and `@SchemaMapping` resolvers
4. Mutations - creating, updating, and deleting data
5. Error handling - custom exceptions, structured errors, partial responses
6. Security & authentication - JWT auth and role-based access
7. Authorization - ownership and field-level access control
8. The N+1 problem & `@BatchMapping` - batch loading with DataLoaders
9. Testing - integration tests with GraphQL-specific assertions
10. Subscriptions - real-time notifications over WebSocket
11. Pagination & filtering - offset and cursor-based pagination
12. Union types & custom scalars - cross-type search and scalar registration
13. External APIs, directives & production - REST/GraphQL API integration, validation directives, instrumentation, production config

## Running the Backend

```bash
./mvnw spring-boot:run
```

- GraphiQL IDE: http://localhost:8080/graphiql
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:moviedb`, user `sa`, empty password)

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

## Seed Users

| Username | Password   | Role    |
|----------|------------|---------|
| `admin`  | `admin123` | `ADMIN` |
| `user`   | `user123`  | `USER`  |

Log in via the `login` mutation (or the frontend), then send `Authorization: Bearer <token>` - admin-only mutations return `FORBIDDEN` otherwise.

## Tech Stack

| Component | Version |
|-----------|---------|
| Spring Boot / Spring for GraphQL | 4.0.5 |
| Spring Security | 7.0.4 |
| Java | 21 |
| Database | H2 (in-memory) |
| Frontend | React + Vite |

## TMDB API Key (Optional)

The app integrates with the real [TMDB](https://www.themoviedb.org) API for the `tmdbSearch` query and the live `communityRating` field. **Everything runs fine without a key** - those features simply return no data - but to see live results you'll need one.

It's free: create an account at [themoviedb.org](https://www.themoviedb.org), then open **Settings → API** and request a key (pick the "Developer" use case; the form takes a minute). TMDB shows you two credentials on that page: a short v3 "API Key" and a long v4 "API Read Access Token". Our code sends the credential as a Bearer token in the `Authorization` header, so copy the **API Read Access Token** - the long one.

The token is a secret, so it never goes into source code or version control. Set it as an environment variable in the shell where you run the backend:

```bash
export TMDB_API_KEY=eyJhbGciOi...your_token_here
./mvnw spring-boot:run
```
