package com.sovereingschool.back_base.Configurations.Filters;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JwtTokenCookieFilterTest {

    private JwtUtil jwtUtil;
    private JwtTokenCookieFilter jwtTokenCookieFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        jwtTokenCookieFilter = new JwtTokenCookieFilter(jwtUtil);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithRefreshTokenCookie_SetsAuthentication() throws ServletException, IOException {
        Cookie refreshCookie = new Cookie("refreshToken", "validRefresh");
        when(request.getCookies()).thenReturn(new Cookie[] { refreshCookie });
        Authentication authentication = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("validRefresh")).thenReturn(authentication);

        jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInitTokenCookie_SetsAuthentication() throws ServletException, IOException {
        Cookie initCookie = new Cookie("initToken", "validInit");
        when(request.getCookies()).thenReturn(new Cookie[] { initCookie });
        Authentication authentication = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("validInit")).thenReturn(authentication);

        jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithBothCookies_PrioritizesRefreshToken() throws ServletException, IOException {
        Cookie refreshCookie = new Cookie("refreshToken", "validRefresh");
        Cookie initCookie = new Cookie("initToken", "validInit");
        when(request.getCookies()).thenReturn(new Cookie[] { refreshCookie, initCookie });
        Authentication authentication = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("validRefresh")).thenReturn(authentication);

        jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).createAuthenticationFromToken("validRefresh");
        verify(jwtUtil, never()).createAuthenticationFromToken("validInit");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNoCookies_DoesNotSetAuthentication() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithUnrelatedCookies_DoesNotSetAuthentication() throws ServletException, IOException {
        Cookie otherCookie = new Cookie("other", "value");
        when(request.getCookies()).thenReturn(new Cookie[] { otherCookie });

        jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ThrowsBadCredentialsException() throws ServletException, IOException {
        Cookie refreshCookie = new Cookie("refreshToken", "invalid");
        when(request.getCookies()).thenReturn(new Cookie[] { refreshCookie });
        when(jwtUtil.createAuthenticationFromToken("invalid")).thenThrow(new BadCredentialsException("Invalid"));

        assertThrows(BadCredentialsException.class, () -> {
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);
        });

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }
}
