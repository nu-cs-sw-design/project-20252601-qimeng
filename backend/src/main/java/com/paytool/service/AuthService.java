package com.paytool.service;

import com.paytool.exception.CustomException;
import com.paytool.model.AuthPayload;
import com.paytool.model.User;
import com.paytool.repository.UserRepository;
import com.paytool.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AuthPayload login(String username, String password) {
        logger.debug("Attempting login for username: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Login failed for username: {} - invalid password", username);
            throw new CustomException("Invalid username or password");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        logger.info("User {} logged in successfully", username);
        
        return new AuthPayload(token, user);
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Transactional(readOnly = true)
    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }
}

