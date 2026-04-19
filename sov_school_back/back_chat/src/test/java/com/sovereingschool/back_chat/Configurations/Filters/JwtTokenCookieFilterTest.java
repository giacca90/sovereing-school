package com.sovereingschool.back_chat.Configurations.Filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    private JwtTokenCookieFilter filter;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtTokenCookieFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_NoCookies() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithRefreshToken() throws ServletException, IOException {
        Cookie refreshCookie = new Cookie("refreshToken", "valid-refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[] { refreshCookie });

        Authentication auth = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("valid-refresh-token")).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithInitToken() throws ServletException, IOException {
        Cookie initCookie = new Cookie("initToken", "valid-init-token");
        when(request.getCookies()).thenReturn(new Cookie[] { initCookie });

        Authentication auth = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("valid-init-token")).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithBothTokens_PrefersRefresh() throws ServletException, IOException {
        Cookie initCookie = new Cookie("initToken", "init-token");
        Cookie refreshCookie = new Cookie("refreshToken", "refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[] { initCookie, refreshCookie });

        Authentication auth = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("refresh-token")).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).createAuthenticationFromToken("refresh-token");
        verify(jwtUtil, never()).createAuthenticationFromToken("init-token");
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_AuthenticationException() throws ServletException, IOException {
        Cookie refreshCookie = new Cookie("refreshToken", "invalid-token");
        when(request.getCookies()).thenReturn(new Cookie[] { refreshCookie });

        when(jwtUtil.createAuthenticationFromToken("invalid-token"))
                .thenThrow(new BadCredentialsException("Invalid"));

        assertThrows(BadCredentialsException.class, () -> {
            filter.doFilterInternal(request, response, filterChain);
        });

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_EmptyTokens() throws ServletException, IOException {
        Cookie initCookie = new Cookie("initToken", " ");
        Cookie refreshCookie = new Cookie("refreshToken", "");
        when(request.getCookies()).thenReturn(new Cookie[] { initCookie, refreshCookie });

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).createAuthenticationFromToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_IrrelevantCookies() throws ServletException, IOException {
        Cookie otherCookie = new Cookie("sessionID", "12345");
        when(request.getCookies()).thenReturn(new Cookie[] { otherCookie });

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).createAuthenticationFromToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_BlankRefreshValidInit() throws ServletException, IOException {
        Cookie initCookie = new Cookie("initToken", "valid-init");
        Cookie refreshCookie = new Cookie("refreshToken", "  ");
        when(request.getCookies()).thenReturn(new Cookie[] { initCookie, refreshCookie });

        Authentication auth = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("valid-init")).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).createAuthenticationFromToken("valid-init");
        verify(jwtUtil, never()).createAuthenticationFromToken("  ");
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_BlankInitValidRefresh() throws ServletException, IOException {
        Cookie initCookie = new Cookie("initToken", "");
        Cookie refreshCookie = new Cookie("refreshToken", "valid-refresh");
        when(request.getCookies()).thenReturn(new Cookie[] { initCookie, refreshCookie });

        Authentication auth = mock(Authentication.class);
        when(jwtUtil.createAuthenticationFromToken("valid-refresh")).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).createAuthenticationFromToken("valid-refresh");
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }
}
