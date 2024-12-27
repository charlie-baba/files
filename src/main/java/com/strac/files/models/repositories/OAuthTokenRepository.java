package com.strac.files.models.repositories;

import com.strac.files.models.OAuthToken;
import com.strac.files.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Charles on 22/12/2024
 */

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long>  {

    Optional<OAuthToken> findTopByUser_OauthIdOrderByCreatedAtDesc(String UserId);
}
