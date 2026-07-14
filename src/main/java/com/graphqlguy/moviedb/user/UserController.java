package com.graphqlguy.moviedb.user;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @MutationMapping
    AuthResponse login(@Argument LoginInput input) {
        return userService.login(input);
    }

    @MutationMapping
    AuthResponse register(@Argument RegisterInput input) {
        return userService.register(input);
    }

}
