package com.graphqlguy.moviedb.user;

import com.graphqlguy.moviedb.config.LatencySimulator;
import com.graphqlguy.moviedb.exception.InvalidInputException;
import com.graphqlguy.moviedb.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LatencySimulator latencySimulator;

    public UserService(final UserRepository userRepository, final PasswordEncoder passwordEncoder,
                       final JwtUtil jwtUtil,
                       final LatencySimulator latencySimulator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;        this.latencySimulator = latencySimulator;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginInput input) {
        latencySimulator.pause();
        AppUser user = userRepository.findByUsername(input.username())
                .orElseThrow(() -> new InvalidInputException("credentials", "Invalid credentials"));
        if (!passwordEncoder.matches(input.password(), user.getPassword())) {
            throw new InvalidInputException("credentials", "Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user);
    }

    @Transactional
    public AuthResponse register(RegisterInput input) {
        latencySimulator.pause();
        if (userRepository.existsByUsername(input.username())) {
            throw new InvalidInputException("username", "Username is already taken");
        }
        if (userRepository.existsByEmail(input.email())) {
            throw new InvalidInputException("email", "Email is already registered");
        }

        AppUser user = userRepository.save(AppUser.builder()
                .username(input.username())
                .email(input.email())
                .password(passwordEncoder.encode(input.password()))
                .role(Role.USER)
                .build());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user);
    }
}