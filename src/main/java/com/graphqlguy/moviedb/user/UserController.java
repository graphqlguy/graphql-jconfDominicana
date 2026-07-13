package com.graphqlguy.moviedb.user;

import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @MutationMapping
    AuthResponse login(@Argument LoginInput input) {
        return userService.login(input);
    }

    @MutationMapping
    AuthResponse register(@Argument RegisterInput input) {
        return userService.register(input);
    }

    // Field-level authorization: an email is visible only to an admin or to the
    // user themselves. Everyone else (another signed-in user, or an anonymous
    // caller) sees null. Because this is a field resolver rather than @PreAuthorize,
    // a hidden email is a quiet null, not an error, so a page of reviews still
    // renders fully for everyone.
    @SchemaMapping(typeName = "User")
    String email(AppUser user, Principal principal) {
        if (principal instanceof Authentication auth) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isSelf = auth.getName().equals(user.getUsername());
            if (isAdmin || isSelf) {
                return user.getEmail();
            }
        }
        return null;
    }

}
