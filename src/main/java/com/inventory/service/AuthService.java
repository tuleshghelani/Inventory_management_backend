package com.inventory.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public UserMaster register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        UserMaster userMaster = new UserMaster();
        userMaster.setEmail(request.getEmail());
        userMaster.setPassword(passwordEncoder.encode(request.getPassword()));
        userMaster.setFirstName(request.getFirstName());
        userMaster.setLastName(request.getLastName());
        return userRepository.save(userMaster);
    }

    public Map<String, String> login(LoginRequest request) throws ValidationException {
        try {
            // Check if user exists and is locked logic BEFORE authentication
            UserMaster user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid username or password"));

            if (user.getLockTime() != null) {
                 if (user.getLockTime().isAfter(OffsetDateTime.now())) {
                     throw new ValidationException("Account is locked. Please try again after 10 minutes", HttpStatus.FORBIDDEN);
                 } else {
                     // Unlock
                     user.setLockTime(null);
                     user.setFailLoginCount(0);
                     userRepository.save(user);
                 }
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Authentication successful - reset counters
             if (user.getFailLoginCount() > 0 || user.getLockTime() != null) {
                user.setFailLoginCount(0);
                user.setLockTime(null);
                // We'll save later along with tokens
            }

            String token = tokenProvider.generateToken(authentication);
            String refreshToken = UUID.randomUUID().toString();

            user.setJwtToken(token);
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(OffsetDateTime.now().plusDays(7)); // 7 days expiry
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", token);
            tokens.put("refreshToken", refreshToken);
            tokens.put("firstName", user.getFirstName());
            tokens.put("lastName", user.getLastName());
            tokens.put("email", user.getEmail());
            
            return tokens;
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
             // Handle failed login attempt
             UserMaster user = userRepository.findByEmail(request.getEmail()).orElse(null);
             if (user != null) {
                 int newCount = user.getFailLoginCount() + 1;
                 user.setFailLoginCount(newCount);
                 if (newCount >= 3) {
                     user.setLockTime(OffsetDateTime.now().plusMinutes(10));
                 }
                 userRepository.save(user);
             }
             throw new ValidationException("Invalid username or password", HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (ValidationException ve) {
            // Rethrow validation exceptions (like Account Locked) as is
            throw ve;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
