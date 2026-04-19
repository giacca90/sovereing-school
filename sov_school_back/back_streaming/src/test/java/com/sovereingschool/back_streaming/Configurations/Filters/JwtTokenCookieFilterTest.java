package com.sovereingschool.back_streaming.Configurations.Filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtTokenCookieFilterTest {

    @Nested
    @DisplayName("Tests para doFilterInternal")
    class DoFilterInternalTests {

        /**
         * Prueba que el filtro procesa un refresh token válido correctamente.
         * El SecurityContextHolder debe actualizarse con la autenticación del refresh
         * token.
         */
        @Test
        void doFilterInternal_ShouldProcessValidRefreshToken() throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "validRefreshToken");
            request.setCookies(refreshToken);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtUtil.createAuthenticationFromToken("validRefreshToken")).thenReturn(auth);

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación no debería ser nula");
            assertEquals(auth, SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería coincidir con el refresh token");
            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtUtil, times(1)).createAuthenticationFromToken("validRefreshToken");
        }

        /**
         * Prueba que el filtro procesa un init token válido cuando no hay refresh
         * token.
         * El SecurityContextHolder debe actualizarse con la autenticación del init
         * token.
         */
        @Test
        void doFilterInternal_ShouldProcessValidInitTokenWhenNoRefreshToken() throws ServletException, IOException {
            // Given
            Cookie initToken = new Cookie("initToken", "validInitToken");
            request.setCookies(initToken);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtUtil.createAuthenticationFromToken("validInitToken")).thenReturn(auth);

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación no debería ser nula");
            assertEquals(auth, SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería coincidir con el init token");
            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtUtil, times(1)).createAuthenticationFromToken("validInitToken");
        }

        /**
         * Prueba que el filtro no establece la autenticación si no hay tokens válidos.
         * El SecurityContextHolder debe permanecer vacío.
         */
        @Test
        void doFilterInternal_ShouldNotSetAuthentication_WhenNoValidTokens() throws ServletException, IOException {
            // Given
            request.setCookies(new Cookie("otherCookie", "value"));

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNull(SecurityContextHolder.getContext().getAuthentication(), "La autenticación debería ser nula");
            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtUtil, never()).createAuthenticationFromToken(anyString());
        }

        /**
         * Prueba que el filtro lanza BadCredentialsException cuando el refresh token es
         * inválido.
         * El SecurityContextHolder debe limpiarse.
         */
        @Test
        void doFilterInternal_ShouldThrowBadCredentialsException_WhenRefreshTokenIsInvalid()
                throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "invalidRefreshToken");
            request.setCookies(refreshToken);
            when(jwtUtil.createAuthenticationFromToken(anyString()))
                    .thenThrow(new BadCredentialsException("Token inválido"));

            // When & Then
            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> jwtTokenCookieFilter.doFilterInternal(request, response, filterChain),
                    "Debería lanzar BadCredentialsException para un refresh token inválido");
            assertEquals("Error en JwtTokenCookieFilter: Token inválido", exception.getMessage(),
                    "El mensaje de error debería coincidir");
            assertNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería ser nula después de la excepción");
            verify(filterChain, never()).doFilter(request, response);
        }

        /**
         * Prueba que el filtro lanza BadCredentialsException cuando el init token es
         * inválido.
         * El SecurityContextHolder debe limpiarse.
         */
        @Test
        void doFilterInternal_ShouldThrowBadCredentialsException_WhenInitTokenIsInvalid()
                throws ServletException, IOException {
            // Given
            Cookie initToken = new Cookie("initToken", "invalidInitToken");
            request.setCookies(initToken);
            when(jwtUtil.createAuthenticationFromToken(anyString()))
                    .thenThrow(new BadCredentialsException("Token inválido"));

            // When & Then
            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> jwtTokenCookieFilter.doFilterInternal(request, response, filterChain),
                    "Debería lanzar BadCredentialsException para un init token inválido");
            assertEquals("Error en JwtTokenCookieFilter: Token inválido", exception.getMessage(),
                    "El mensaje de error debería coincidir");
            assertNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería ser nula después de la excepción");
            verify(filterChain, never()).doFilter(request, response);
        }

        /**
         * Prueba que el filtro maneja múltiples cookies correctamente, priorizando el
         * refresh token.
         */
        @Test
        void doFilterInternal_ShouldPrioritizeRefreshTokenOverInitToken() throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "validRefreshToken");
            Cookie initToken = new Cookie("initToken", "validInitToken");
            request.setCookies(refreshToken, initToken);
            Authentication refreshAuth = new UsernamePasswordAuthenticationToken("refreshUser", null, null);
            when(jwtUtil.createAuthenticationFromToken("validRefreshToken")).thenReturn(refreshAuth);

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación no debería ser nula");
            assertEquals(refreshAuth, SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería ser del refresh token");
            verify(jwtUtil, times(1)).createAuthenticationFromToken("validRefreshToken");
            verify(jwtUtil, never()).createAuthenticationFromToken("validInitToken"); // initToken no debe ser procesado
            verify(filterChain, times(1)).doFilter(request, response);
        }

        /**
         * Prueba que el filtro maneja un request sin cookies.
         * El SecurityContextHolder debe permanecer vacío.
         */
        @Test
        void doFilterInternal_ShouldHandleNoCookies() throws ServletException, IOException {
            // Given: No cookies set

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNull(SecurityContextHolder.getContext().getAuthentication(), "La autenticación debería ser nula");
            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtUtil, never()).createAuthenticationFromToken(anyString());
        }

        /**
         * Prueba la excepción general de IOException durante la cadena de filtros.
         */
        @Test
        void doFilterInternal_ShouldHandleIOExceptionFromFilterChain() throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "validRefreshToken");
            request.setCookies(refreshToken);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtUtil.createAuthenticationFromToken("validRefreshToken")).thenReturn(auth);
            org.mockito.Mockito.doThrow(new IOException("Filter chain IO error")).when(filterChain).doFilter(request,
                    response);

            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> jwtTokenCookieFilter.doFilterInternal(request, response, filterChain),
                    "Debería lanzar IOException desde la cadena de filtros");
            assertEquals("Filter chain IO error", exception.getMessage(), "El mensaje de error debería coincidir");
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería persistir antes de la excepción de IO");
        }

        /**
         * Prueba la excepción general de ServletException durante la cadena de filtros.
         */
        @Test
        void doFilterInternal_ShouldHandleServletExceptionFromFilterChain() throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "validRefreshToken");
            request.setCookies(refreshToken);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtUtil.createAuthenticationFromToken("validRefreshToken")).thenReturn(auth);
            org.mockito.Mockito.doThrow(new ServletException("Filter chain Servlet error")).when(filterChain)
                    .doFilter(request, response);

            // When & Then
            ServletException exception = assertThrows(ServletException.class,
                    () -> jwtTokenCookieFilter.doFilterInternal(request, response, filterChain),
                    "Debería lanzar ServletException desde la cadena de filtros");
            assertEquals("Filter chain Servlet error", exception.getMessage(), "El mensaje de error debería coincidir");
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería persistir antes de la excepción de Servlet");
        }

        /**
         * Prueba que el filtro ignora cookies con valores vacíos o nulos.
         */
        @Test
        void doFilterInternal_ShouldIgnoreEmptyOrNullCookieValues() throws ServletException, IOException {
            // Given
            Cookie emptyRefreshToken = new Cookie("refreshToken", "");
            Cookie nullInitToken = new Cookie("initToken", null);
            Cookie validOtherToken = new Cookie("otherToken", "value");
            request.setCookies(emptyRefreshToken, nullInitToken, validOtherToken);

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNull(SecurityContextHolder.getContext().getAuthentication(), "La autenticación debería ser nula");
            verify(jwtUtil, never()).createAuthenticationFromToken(anyString());
            verify(filterChain, times(1)).doFilter(request, response);
        }

        /**
         * Prueba que el filtro procesa un refresh token válido y un init token vacío.
         */
        @Test
        void doFilterInternal_ShouldProcessRefreshTokenWithEmptyInitToken() throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "validRefreshToken");
            Cookie initToken = new Cookie("initToken", "");
            request.setCookies(refreshToken, initToken);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtUtil.createAuthenticationFromToken("validRefreshToken")).thenReturn(auth);

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación no debería ser nula");
            assertEquals(auth, SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería coincidir con el refresh token");
            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtUtil, times(1)).createAuthenticationFromToken("validRefreshToken");
            verify(jwtUtil, never()).createAuthenticationFromToken("");
        }

        /**
         * Prueba que el filtro procesa un init token válido y un refresh token vacío.
         */
        @Test
        void doFilterInternal_ShouldProcessInitTokenWithEmptyRefreshToken() throws ServletException, IOException {
            // Given
            Cookie refreshToken = new Cookie("refreshToken", "");
            Cookie initToken = new Cookie("initToken", "validInitToken");
            request.setCookies(refreshToken, initToken);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtUtil.createAuthenticationFromToken("validInitToken")).thenReturn(auth);

            // When
            jwtTokenCookieFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación no debería ser nula");
            assertEquals(auth, SecurityContextHolder.getContext().getAuthentication(),
                    "La autenticación debería coincidir con el init token");
            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtUtil, times(1)).createAuthenticationFromToken("validInitToken");
            verify(jwtUtil, never()).createAuthenticationFromToken("");
        }
    }

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtTokenCookieFilter jwtTokenCookieFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }
}