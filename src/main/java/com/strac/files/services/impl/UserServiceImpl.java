package com.strac.files.services.impl;

import com.strac.files.models.User;
import com.strac.files.models.repositories.UserRepository;
import com.strac.files.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Charles on 05/01/2025
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public User findUserByOauthId(String oauthId) {
        return userRepository.findByOauthId(oauthId);
    }
}
