package com.strac.files.services.impl;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.strac.files.models.OAuthToken;
import com.strac.files.models.User;
import com.strac.files.models.repositories.OAuthTokenRepository;
import com.strac.files.models.repositories.UserRepository;
import com.strac.files.services.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * @author Charles on 22/12/2024
 */
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    @Value("${google.oauth.client.id}")
    private String clientId;

    @Value("${google.oauth.client.secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect.uri}")
    private String redirectUri;

    @Value("${google.oauth.base.uri}")
    private String baseUri;

    @Value("${google.token.server.uri}")
    private String tokenServerUri;

    @Autowired
    private GoogleAuthorizationCodeFlow flow;
    private final UserRepository userRepository;
    private final OAuthTokenRepository tokenRepository;

    public String getOauthId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (String) authentication.getPrincipal();
    }

    @Override
    public String createAuthorizationUrl() {
        String scopes = String.join(" ", Arrays.asList(
                DriveScopes.DRIVE,
                "email",
                "profile",
                "openid"
        ));
        return baseUri +
                "client_id=" + clientId +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8) +
                "&redirect_uri=" + redirectUri +
                "&access_type=offline";
    }

    @Override
    public User processGoogleCallback(String code) throws IOException {
        // use  the response code from google to generate token response, which contains user details
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        GoogleIdToken idToken = tokenResponse.parseIdToken();
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String oauthId = payload.getSubject();

        // use the response to create credential and store it in the GoogleAuthorizationCodeFlow
        flow.createAndStoreCredential(tokenResponse, oauthId);

        // Save or update user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setOauthId(oauthId);
                    newUser.setEmail(email);
                    return userRepository.save(newUser);
                });

        // Save refresh token for user
        if (tokenResponse.getRefreshToken() != null) {
            OAuthToken oauthToken = new OAuthToken();
            oauthToken.setUser(user);
            oauthToken.setAccessToken(tokenResponse.getAccessToken());
            oauthToken.setRefreshToken(tokenResponse.getRefreshToken());
            oauthToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
            tokenRepository.save(oauthToken);
        }

        return user;
    }

    // This uses the stored refresh token to call google api, which returns a new access token
    @Override
    public String refreshAccessToken(OAuthToken token) throws IOException {
        RefreshTokenRequest request =  new RefreshTokenRequest(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                new GenericUrl(tokenServerUri),
                token.getRefreshToken());

        // Add clientId and clientSecret directly to the request parameters
        request.put("client_id", clientId);
        request.put("client_secret", clientSecret);

        TokenResponse response = request.execute();

        // Update token in database
        token.setAccessToken(response.getAccessToken());
        token.setExpiryDate(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
        tokenRepository.save(token);

        return response.getAccessToken();
    }
}
