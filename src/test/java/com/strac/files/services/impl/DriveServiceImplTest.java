package com.strac.files.services.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.strac.files.exceptions.UnauthorizedException;
import com.strac.files.models.OAuthToken;
import com.strac.files.models.User;
import com.strac.files.models.repositories.OAuthTokenRepository;
import com.strac.files.services.OAuth2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Charles on 23/12/2024
 */

@ExtendWith(MockitoExtension.class)
class DriveServiceImplTest {

    @Mock
    private GoogleAuthorizationCodeFlow flow;

    @Mock
    private OAuth2Service authService;

    @Mock
    private OAuthTokenRepository tokenRepository;

    @Mock
    private HttpTransport httpTransport;

    @Mock
    private JsonFactory jsonFactory;

    @InjectMocks
    private DriveServiceImpl driveService;

    private static final String USER_ID = "test-user-id";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";

    private OAuthToken validToken;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setOauthId(USER_ID);

        validToken = new OAuthToken();
        validToken.setUser(user);
        validToken.setAccessToken(ACCESS_TOKEN);
        validToken.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getDriveService_WithValidNonExpiredToken_ReturnsDriveService() throws IOException {
        // Arrange
        validToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        when(tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(validToken));
        when(flow.getTransport()).thenReturn(httpTransport);
        when(flow.getJsonFactory()).thenReturn(jsonFactory);

        // Act
        Drive result = driveService.getDriveService(USER_ID);

        // Assert
        assertNotNull(result);
        verify(tokenRepository).findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID);
        verify(authService, never()).refreshAccessToken(any());
    }

    @Test
    void getDriveService_WithExpiredToken_RefreshesAndReturnsDriveService() throws IOException {
        // Arrange
        validToken.setExpiryDate(LocalDateTime.now().minusMinutes(5));
        when(tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(validToken));
        when(authService.refreshAccessToken(validToken)).thenReturn(NEW_ACCESS_TOKEN);
        when(flow.getTransport()).thenReturn(httpTransport);
        when(flow.getJsonFactory()).thenReturn(jsonFactory);

        // Act
        Drive result = driveService.getDriveService(USER_ID);

        // Assert
        assertNotNull(result);
        verify(tokenRepository).findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID);
        verify(authService).refreshAccessToken(validToken);
    }

    @Test
    void getDriveService_WithTokenAboutToExpire_RefreshesAndReturnsDriveService() throws IOException {
        // Arrange
        validToken.setExpiryDate(LocalDateTime.now().plusSeconds(30)); // Will expire in 30 seconds
        when(tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(validToken));
        when(authService.refreshAccessToken(validToken)).thenReturn(NEW_ACCESS_TOKEN);
        when(flow.getTransport()).thenReturn(httpTransport);
        when(flow.getJsonFactory()).thenReturn(jsonFactory);

        // Act
        Drive result = driveService.getDriveService(USER_ID);

        // Assert
        assertNotNull(result);
        verify(tokenRepository).findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID);
        verify(authService).refreshAccessToken(validToken);
    }

    @Test
    void getDriveService_WithNoToken_ThrowsUnauthorizedException() {
        // Arrange
        when(tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> driveService.getDriveService(USER_ID));
        assertEquals("No token found for user", exception.getMessage());
    }

    @Test
    void getDriveService_WhenTokenRefreshFails_ThrowsUnauthorizedException() throws IOException {
        // Arrange
        validToken.setExpiryDate(LocalDateTime.now().minusMinutes(5));
        when(tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(validToken));
        when(authService.refreshAccessToken(validToken)).thenThrow(new IOException("Refresh failed"));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> driveService.getDriveService(USER_ID));
        assertEquals("Failed to refresh token for user "+ USER_ID, exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void getDriveService_WithNullUserId_ThrowsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> driveService.getDriveService(null));
        assertEquals("userId cannot be null", exception.getMessage());
    }

    @Test
    void getDriveService_WithEmptyUserId_ThrowsUnauthorizedException() {
        // Arrange
        when(tokenRepository.findTopByUser_OauthIdOrderByCreatedAtDesc(""))
                .thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> driveService.getDriveService(""));
        assertEquals("No token found for user", exception.getMessage());
    }
}