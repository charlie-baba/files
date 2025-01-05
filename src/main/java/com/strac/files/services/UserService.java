package com.strac.files.services;

import com.strac.files.models.User;

/**
 * @author Charles on 05/01/2025
 */
public interface UserService {

    User findUserByEmail(String email);

    User findUserByOauthId(String oauthId);
}
