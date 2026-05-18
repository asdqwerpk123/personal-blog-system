package org.example.personalblogsystem.filter;

import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.util.JwtUtil;
import org.example.personalblogsystem.utils.RedisCache;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Backward-compatible class name kept for existing imports.
 */
public class JwtAuthenticationTokenFilter extends JwtAuthenticationFilter {

    public JwtAuthenticationTokenFilter(JwtTokenService jwtTokenService,
                                        RedisCache redisCache,
                                        JwtUtil jwtUtil,
                                        UserDetailsService userDetailsService) {
        super(jwtTokenService, redisCache, jwtUtil, userDetailsService);
    }
}
