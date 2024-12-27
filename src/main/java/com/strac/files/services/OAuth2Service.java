package com.strac.files.services;

import com.strac.files.models.OAuthToken;
import com.strac.files.models.User;

import java.io.IOException;

/**
 * @author Charles on 22/12/2024
 */
public interface OAuth2Service {

    String getOauthId();

    String createAuthorizationUrl();

    User processGoogleCallback(String code) throws IOException;

    String refreshAccessToken(OAuthToken token) throws IOException;
}
