package com.blastradius.service;

import com.blastradius.dto.AuthResponse;
import com.blastradius.dto.LoginRequest;
import com.blastradius.dto.RegisterRequest;
import com.blastradius.entity.User;
import com.blastradius.entity.User.Role;
import com.blastradius.exception.BadRequestException;
import com.blastradius.exception.ConflictException;
import com.blastradius.repository.UserRepository;
import com.blastradius.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration, login, and token management.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }

        Role role = Role.USER;
        if ("ADMIN".equalsIgnoreCase(request.getRole())) {
            role = Role.ADMIN;
        }

        User user = new User(request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                role);
        userRepository.save(user);
        log.info("Registered new user: {} with role: {}", user.getUsername(), user.getRole());

        String accessToken = jwtTokenProvider.generateTokenFromUsername(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        return new AuthResponse(accessToken, refreshToken, user.getUsername(),
                user.getRole().name(), jwtTokenProvider.getJwtExpiration());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String accessToken = jwtTokenProvider.generateToken(auth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        log.info("User logged in: {}", request.getUsername());
        return new AuthResponse(accessToken, refreshToken, user.getUsername(),
                user.getRole().name(), jwtTokenProvider.getJwtExpiration());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return new AuthResponse(newAccessToken, newRefreshToken, username,
                user.getRole().name(), jwtTokenProvider.getJwtExpiration());
    }
}
