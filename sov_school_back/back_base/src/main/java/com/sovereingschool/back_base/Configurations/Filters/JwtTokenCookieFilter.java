package com.sovereingschool.back_base.Configurations.Filters;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtTokenCookieFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtTokenCookieFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Cookie[] cookies = req.getCookies();
            String init = "";
            String refresh = "";
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (c.getName().equals("refreshToken") && hasText(c.getValue())) {
                        refresh = c.getValue();
                    }
                    if (c.getName().equals("initToken") && hasText(c.getValue())) {
                        init = c.getValue();
                    }
                }
            }
            if (!refresh.isEmpty()) {
                Authentication auth = jwtUtil.createAuthenticationFromToken(refresh);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else if (!init.isEmpty()) {
                Authentication auth = jwtUtil.createAuthenticationFromToken(init);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            throw new BadCredentialsException("Error en JwtTokenCookieFilter: " + ex.getMessage());
        }
        filterChain.doFilter(req, res);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
