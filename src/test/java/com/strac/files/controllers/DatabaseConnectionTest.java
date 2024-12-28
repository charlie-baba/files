package com.strac.files.controllers;

import com.strac.files.models.User;
import com.strac.files.models.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Charles on 22/12/2024
 */

 @Slf4j
@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private UserRepository userRepository;

    // Test Database connection exists
    @Test
    void testDatabaseConnection() {
        assertDoesNotThrow(() -> {
            userRepository.count();
        });
    }

    // Test Database read/write permission
    @Test
    void testUserCreation() {
        String email = "test@example.com" + System.currentTimeMillis();
        User user = new User();
        user.setEmail(email);
        user.setOauthId("752348970237");
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        Optional<User> foundUser = userRepository.findByEmail(email);
        assertTrue(foundUser.isPresent());

        // Clean up
        try {
            userRepository.delete(savedUser);
        } catch (Exception e) {
            log.error("Error deleting test user: {}", e.getMessage());
        }
    }
}
