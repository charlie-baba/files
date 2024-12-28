package com.strac.files.interceptors;

import com.strac.files.exceptions.UnauthorizedException;
import com.strac.files.models.User;
import com.strac.files.models.repositories.UserRepository;
import com.strac.files.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author Charles on 22/12/2024
 *
 * This class intecepts any request, extracts the oauthId from the Jwt in the header
 * and adds it to the HttpServletRequest
 */

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private static final String BEARER_PREFIX = "Bearer ";

    // For every request, this checks the jwt
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        String oauthId = null;
        String jwt = null;

        // Extract oauthId from  token
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            jwt = authorizationHeader.substring(BEARER_PREFIX.length());
            oauthId = jwtUtil.extractIdFromToken(jwt);
        }

        if (oauthId != null) {
            User user = userRepository.findByOauthId(oauthId);
            if (user == null || !jwtUtil.validateToken(jwt, user.getOauthId())) {
                throw new UnauthorizedException("Invalid token");
            }

            Authentication auth = new UsernamePasswordAuthenticationToken(oauthId, null, null);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
