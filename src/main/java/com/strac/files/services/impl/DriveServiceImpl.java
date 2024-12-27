package com.strac.files.services.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;
import com.strac.files.exceptions.UnauthorizedException;
import com.strac.files.models.OAuthToken;
import com.strac.files.models.repositories.OAuthTokenRepository;
import com.strac.files.services.DriveService;
import com.strac.files.services.OAuth2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * @author Charles on 25/12/2024
 */

@Service
public class DriveServiceImpl implements DriveService {

    @Autowired
    private GoogleAuthorizationCodeFlow flow;

    @Autowired
    private OAuth2Service authService;

    @Autowired
    private OAuthTokenRepository tokenRepository;

    public Drive getDriveService(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        OAuthToken storedToken = tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new UnauthorizedException("No token found for user"));

        // Check if token is expired or about to expire (within 60 seconds)
        if (storedToken.getExpiryDate().minusSeconds(60).isBefore(LocalDateTime.now())) {
            try {
                String newAccessToken = authService.refreshAccessToken(storedToken);
                storedToken.setAccessToken(newAccessToken);
            } catch (IOException e) {
                throw new UnauthorizedException("Failed to refresh token for user "+ userId, e);
            }
        }

        // Create and return Drive service using stored/refreshed token
        return new Drive.Builder(
                flow.getTransport(),
                flow.getJsonFactory(),
                new GoogleCredential().setAccessToken(storedToken.getAccessToken()))
                .setApplicationName("Strac Google Drive Demo")
                .build();
    }
}
