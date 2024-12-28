package com.strac.files.services.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.strac.files.models.OAuthToken;
import com.strac.files.models.User;
import com.strac.files.models.repositories.OAuthTokenRepository;
import com.strac.files.models.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Charles on 23/12/2024
 */

@ExtendWith(MockitoExtension.class)
class OAuth2ServiceImplTest {

    @Mock
    private GoogleAuthorizationCodeFlow flow;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthTokenRepository tokenRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2ServiceImpl oAuth2Service;

    private final String CLIENT_ID = "test-client-id";
    private final String CLIENT_SECRET = "test-client-secret";
    private final String REDIRECT_URI = "http://localhost:8080/callback";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2Service, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(oAuth2Service, "clientSecret", CLIENT_SECRET);
        ReflectionTestUtils.setField(oAuth2Service, "redirectUri", REDIRECT_URI);
        ReflectionTestUtils.setField(oAuth2Service, "baseUri", "https://accounts.google.com/o/oauth2/v2/auth?");
        ReflectionTestUtils.setField(oAuth2Service, "tokenServerUri", "https://oauth2.googleapis.com/token");
    }

    @Test
    void getOauthId_Success() {
        // Arrange
        String expectedOauthId = "test-oauth-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(expectedOauthId);

        // Act
        String actualOauthId = oAuth2Service.getOauthId();

        // Assert
        assertEquals(expectedOauthId, actualOauthId);
        verify(authentication).getPrincipal();
    }

    @Test
    void createAuthorizationUrl_ReturnsValidUrl() {
        // Act
        String authUrl = oAuth2Service.createAuthorizationUrl();

        // Assert
        assertNotNull(authUrl);
        assertTrue(authUrl.contains("client_id=test-client-id"));
        assertTrue(authUrl.contains("response_type=code"));
        assertTrue(authUrl.contains("redirect_uri=http://localhost:8080/callback"));
        assertTrue(authUrl.contains("access_type=offline"));
        assertTrue(authUrl.contains("scope="));
    }

    @Test
    void processGoogleCallback_NewUser_Success() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Arrange
        String testCode = "test-auth-code";
        String testEmail = "test@example.com";
        String testOauthId = "test-oauth-id";

        // Create mocks
        GoogleTokenResponse tokenResponse = getGoogleTokenResponse(testEmail, testOauthId, testCode);

        when(tokenResponse.getRefreshToken()).thenReturn("refresh-token");
        when(tokenResponse.getAccessToken()).thenReturn("access-token");
        when(tokenResponse.getExpiresInSeconds()).thenReturn(60L);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(tokenRepository.save(any(OAuthToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = oAuth2Service.processGoogleCallback(testCode);

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testOauthId, result.getOauthId());
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(OAuthToken.class));
    }

    @Test
    void processGoogleCallback_ExistingUser_Success() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Arrange
        String testCode = "test-auth-code";
        String testEmail = "test@example.com";
        String testOauthId = "test-oauth-id";

        User existingUser = new User();
        existingUser.setEmail(testEmail);
        existingUser.setOauthId(testOauthId);

        getGoogleTokenResponse(testEmail, testOauthId, testCode);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

        // Act
        User result = oAuth2Service.processGoogleCallback(testCode);

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testOauthId, result.getOauthId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processGoogleCallback_NoRefreshToken_Success() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Arrange
        String testCode = "test-auth-code";
        String testEmail = "test@example.com";
        String testOauthId = "test-oauth-id";

        getGoogleTokenResponse(testEmail, testOauthId, testCode);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = oAuth2Service.processGoogleCallback(testCode);

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testOauthId, result.getOauthId());
        verify(tokenRepository, never()).save(any(OAuthToken.class));
    }

    private GoogleTokenResponse getGoogleTokenResponse(String testEmail, String testOauthId, String testCode)
            throws IOException, NoSuchFieldException, IllegalAccessException {
        GoogleAuthorizationCodeTokenRequest tokenRequest = mock(GoogleAuthorizationCodeTokenRequest.class);
        GoogleTokenResponse tokenResponse = mock(GoogleTokenResponse.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);

        when(payload.getEmail()).thenReturn(testEmail);
        when(payload.getSubject()).thenReturn(testOauthId);
        when(idToken.getPayload()).thenReturn(payload);

        when(tokenResponse.parseIdToken()).thenReturn(idToken);
        when(flow.createAndStoreCredential(any(GoogleTokenResponse.class), anyString()))
                .thenReturn(mock(Credential.class));

        // Mock the token request process
        when(flow.newTokenRequest(testCode)).thenReturn(tokenRequest);
        when(tokenRequest.setRedirectUri(anyString())).thenReturn(tokenRequest);
        when(tokenRequest.execute()).thenReturn(tokenResponse);

        // Inject `flow` mock
        Field flowField = OAuth2ServiceImpl.class.getDeclaredField("flow");
        flowField.setAccessible(true);
        flowField.set(oAuth2Service, flow);
        return tokenResponse;
    }
}
