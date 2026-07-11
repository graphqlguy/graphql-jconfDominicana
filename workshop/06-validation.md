# Class 6: Input Validation

The mutations we have written so far trust their input completely. `createMovie` accepts an empty title and a release year of 3000 and stores both without objection. A production API cannot make that assumption: input arriving over the network must be checked before it is acted upon. This class adds that checking, with an emphasis on doing it declaratively, in the schema, where the rules become part of the published contract.

Validation in a Spring for GraphQL application happens in three distinct places, and it is worth being clear about which belongs where:

- **Schema directives** constrain the *shape* of a value: its length, its numeric range, its format. These rules are declared in the schema, visible through introspection, and enforced before any resolver runs. This is the layer we build in this class.
- **Java Bean Validation** (JSR-303: `@Valid` with Jakarta annotations) places the same kind of field-level constraint on the Java records instead of the schema. We have already used it, on `createPerson`.
- **Service-layer validation** enforces rules that neither of the outer layers can express: business rules that depend on the current state of the database, or on more than one field at once. These must live in the service.

Together the three form a graduated defense: the outer layers reject malformed input cheaply and early, while the service layer enforces the domain rules that require real knowledge of the system. We focus on the schema layer and address the others briefly.

By the end of this class, you will:

- Add and wire the GraphQL extended-validation library
- Constrain input fields declaratively with schema directives
- Understand which validation belongs in each of the three layers

## 1. The problem

`createMovie` is an administrator-only mutation, so in GraphiQL set your admin token in the **Headers** tab as you did in class 4:

```json
{ "Authorization": "Bearer <admin token>" }
```

Then submit deliberately invalid input:

```graphql
mutation {
  createMovie(input: { title: "", releaseYear: 3000, genre: DRAMA }) {
    id
    title
  }
}
```

The movie is created. An empty title and an impossible year pass straight through to the database.

> [!NOTE]
> The frontend's Add Movie form will not let you reproduce this: its fields carry HTML constraints (`required`, `min`, `max`) that block bad input in the browser. That protection is real but partial. It applies only to that one form in a compliant browser; a direct API call, from GraphiQL or any other client, ignores it entirely. Client-side checks improve the user experience; they are not a substitute for validation on the server.

## 2. Add the extended-validation library

Constraining input length and range is such a universal need that the constraints themselves have been standardized and packaged. Rather than write the same length and range checks by hand on every field, we use `graphql-java-extended-validation`, a library that provides a set of ready-made validators, `@Size`, `@Range`, `@Pattern`, and more, as reusable **schema directives**. We declare a directive on a field, and the library enforces it.

Add the dependency:

`pom.xml`

```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-extended-validation</artifactId>
    <version>24.0</version>
</dependency>
```

## 3. Wire it in

The library supplies the validation rules; we register them with Spring for GraphQL's runtime wiring through a configuration bean:

`src/main/java/com/graphqlguy/moviedb/config/GraphQLConfig.java`

```java
package com.graphqlguy.moviedb.config;

import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        ValidationRules validationRules = ValidationRules.newValidationRules()
                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
                .build();
        return wiringBuilder -> wiringBuilder
                .directiveWiring(new ValidationSchemaWiring(validationRules));
    }
}
```

`RETURN_NULL` instructs the library that, when a field fails validation, it resolves to `null` and the failure is reported in the `errors` array, the partial-response behavior from class 5.

## 4. Declare and apply the directives

A directive must be declared before it can be used. Add the two definitions at the top of the schema:

`src/main/resources/graphql/schema.graphqls`

```graphql
directive @Range(min: Int = 0, max: Int = 2147483647, message: String = "graphql.validation.Range.message") on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @Size(min: Int = 0, max: Int = 2147483647, message: String = "graphql.validation.Size.message") on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
```

Now constrain the fields of `CreateMovieInput`:

```graphql
input CreateMovieInput {
    title: String! @Size(min: 1, max: 200)
    releaseYear: Int! @Range(min: 1888, max: 2100)
    genre: Genre!
    """Rating from 0.0 to 10.0"""
    rating: Float @Range(min: 0, max: 10)
    """Runtime in minutes"""
    runtime: Int @Range(min: 1, max: 600)
    plot: String @Size(max: 2000)
    posterUrl: String @Size(max: 500)
    tmdbId: Int
}
```

The rules are now part of the schema. A client inspecting the API learns not only that `title` is a `String!`, but that its length must fall between 1 and 200. The constraint documents itself.

## 5. Verify

Restart and submit the invalid mutation from step 1 again:

```json
{
  "errors": [ {
    "message": "/createMovie/input/title size must be between 1 and 200",
    "path": ["createMovie"],
    "extensions": {
      "classification": { "type": "ExtendedValidationError", "constraint": "@Size" }
    }
  } ],
  "data": { "createMovie": null }
}
```

The mutation is rejected before it reaches the service, and the error names the exact field and constraint that failed. Valid input is unaffected:

```graphql
mutation {
  createMovie(input: { title: "A Fistful of Dollars", releaseYear: 1964, genre: WESTERN }) {
    id
    title
  }
}
```

## 6. The other two layers

Schema directives cover format and range, but not every rule fits that mould.

**Java Bean Validation** applies field-level constraints on the Java side. `createPerson` already uses it: `CreatePersonInput` is annotated with Jakarta constraints, and the controller argument is marked `@Valid`.

```java
public record CreatePersonInput(
        @NotBlank @Size(max = 100) String name,
        @Min(1850) Integer birthYear,
        @Size(min = 2, max = 2) String countryCode) { }
```

The `@NotBlank` rejects an empty or whitespace-only name; `@Size` bounds its length; `@Min` sets a floor on the birth year.

A violation throws `ConstraintViolationException`, which the handler from class 5 turns into a `BAD_REQUEST`. Bean Validation is the right choice when the constraint is a server-side concern rather than part of the public contract.

**Service-layer validation** handles what neither outer layer can express: rules that depend on the state of the database or on relationships between entities. These are already present in our services. `updatePerson` rejects a name that is provided but blank; deleting a linked person is refused; a user may not review the same title twice. No directive and no field annotation can state "this person is referenced by a movie," because answering it requires a query. Such rules necessarily live in the service, close to the data.

The guiding principle: validate as far out as the rule allows. Push structural checks to the schema, keep field constraints in Bean Validation where a directive does not fit, and reserve the service layer for the domain rules that genuinely require it.

## Exercise

`UpdateMovieInput` and `RegisterInput` are still unvalidated. Constrain both with the same directives: give `UpdateMovieInput` the field limits of `CreateMovieInput` (its fields are optional, so a value must be valid only when present), and constrain `RegisterInput` so the username, email, and password cannot be empty or excessively long. The `RegisterInput` docstring already promises these rules; this makes the promise true.

<details>
<summary>Show solution</summary>

`src/main/resources/graphql/schema.graphqls`

```graphql
input UpdateMovieInput {
    id: ID!
    title: String @Size(min: 1, max: 200)
    releaseYear: Int @Range(min: 1888, max: 2100)
    genre: Genre
    """Rating from 0.0 to 10.0"""
    rating: Float @Range(min: 0, max: 10)
    """Runtime in minutes"""
    runtime: Int @Range(min: 1, max: 600)
    plot: String @Size(max: 2000)
    posterUrl: String @Size(max: 500)
    tmdbId: Int
}

input RegisterInput {
    username: String @Size(min: 3, max: 50)
    email: String @Size(max: 120)
    password: String @Size(min: 6, max: 100)
}
```

A directive on an optional field is enforced only when the client sends a value, which matches the partial-update semantics from class 3: an omitted field is left unchanged, and a field that is present must satisfy its constraint.

</details>

## Recap

- Validation belongs in three layers: schema directives for structure, Bean Validation for Java-side field constraints, and the service layer for rules that require database state.
- Schema-directive validation, from the `graphql-java-extended-validation` library, makes input rules part of the contract; a `RuntimeWiringConfigurer` bean registers them and a failed rule is reported in the `errors` array.
- Client-side form constraints are a convenience, not a guarantee; the server must validate independently.
- Validate as far out as each rule allows, and no further.
