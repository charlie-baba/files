package com.strac.files.controllers;

import com.strac.files.models.User;
import com.strac.files.services.OAuth2Service;
import com.strac.files.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * @author Charles on 22/12/2024
 * Controller for OAuth2 Authentication
 */

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private final OAuth2Service authService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/login/google")
    public ResponseEntity<String> getLoginUrl() {
        String authUrl = authService.createAuthorizationUrl();
        return ResponseEntity.ok(authUrl);
    }

    // Exchange authorization code for tokens
    @GetMapping("/google/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String code) {
        try {
            User user = authService.processGoogleCallback(code);
            String jwt = jwtUtil.generateToken(user.getOauthId());

            // Ideally would use a token store for production code
            String redirectUrl = String.format("http://localhost:3000/auth-success?token=%s", jwt);

            // Redirect to frontend with success
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/auth-error"))
                    .build();
        }
    }

    // Check if jwt is still valid
    @GetMapping("/check")
    public boolean check() {
        return authService.getOauthId() != null;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        SecurityContextHolder.clearContext();

        // Possibly blacklist the token for bigger projects
        return ResponseEntity.ok("Logged out successfully");
    }
}
