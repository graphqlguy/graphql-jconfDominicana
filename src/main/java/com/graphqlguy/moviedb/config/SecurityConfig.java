package com.graphqlguy.moviedb.config;

import com.graphqlguy.moviedb.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //CSRF is disabled because our API is stateless. CSRF protection guards against attacks where a malicious website submits forms to
                // your server using the victim's session cookie. Since we use JWT tokens (not cookies) and have no sessions, CSRF attacks aren't possible against our API.
                .csrf(csrf -> csrf.disable())
                //Session management is STATELESS because JWT tokens carry all authentication information. The server doesn't need to remember sessions between requests,
                // which is simpler and scales better. Any server in a cluster can validate a JWT without consulting a shared session store.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //All requests are permitted at the HTTP level. This might seem counterintuitive - why have security if everything is allowed? The answer is that we handle
                // authorization at the method level using @PreAuthorize, not at the URL level. GraphQL has one endpoint, and we need unauthenticated users to reach it for public queries like movies.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
