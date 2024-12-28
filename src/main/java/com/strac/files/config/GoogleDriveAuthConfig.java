package com.strac.files.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * @author Charles on 23/12/2024
 *
 * This is a bean that creates GoogleAuthorizationCodeFlow.
 * GoogleAuthorizationCodeFlow creates and stores credentials for accessing google drive api.
 * I made it a separate bean so that it can be Autowired/Injected anywhere it is needed.
 */
@Configuration
public class GoogleDriveAuthConfig {

    @Value("${google.oauth.client.id}")
    private String clientId;

    @Value("${google.oauth.client.secret}")
    private String clientSecret;

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow() throws GeneralSecurityException, IOException {
        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                Arrays.asList(DriveScopes.DRIVE, "email", "profile", "openid")
            )
            .setDataStoreFactory(new MemoryDataStoreFactory())
            .setAccessType("offline")
            .build();
    }
}
