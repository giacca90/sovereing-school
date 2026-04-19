package com.sovereingschool.back_base.Configurations.Filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.Interfaces.ILoginService;
import com.sovereingschool.back_base.Services.UsuarioService;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class CustomOAuth2SuccessHandlerTest {

    private ILoginService loginService;
    private UsuarioService usuarioService;
    private ObjectMapper objectMapper;
    private CustomOAuth2SuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private final String FRONT_URL = "http://localhost:3000";

    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        loginService = mock(ILoginService.class);
        usuarioService = mock(UsuarioService.class);
        objectMapper = new ObjectMapper();
        handler = new CustomOAuth2SuccessHandler(FRONT_URL, loginService, usuarioService, objectMapper);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        // Mock PrintWriter for response
        printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void onAuthenticationSuccess_WhenNotOAuth2User_SendsBadRequest() throws IOException, ServletException {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("notOAuth2User");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    void onAuthenticationSuccess_WhenNoEmail_SendsBadRequest() throws IOException, ServletException {
        OAuth2User oauthUser = mock(OAuth2User.class);
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttributes()).thenReturn(new HashMap<>());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    void onAuthenticationSuccess_WhenUserExists_RedirectsToFront() throws Exception {
        OAuth2User oauthUser = mock(OAuth2User.class);
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oauthUser);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@test.com");
        when(oauthUser.getAttributes()).thenReturn(attributes);

        when(loginService.compruebaCorreo("test@test.com")).thenReturn(1L);
        when(loginService.getPasswordLogin(1L)).thenReturn("password");
        AuthResponse authResponse = new AuthResponse(true, "OK", new Usuario(), "accessToken", "refreshToken");
        when(loginService.loginUser(1L, "password")).thenReturn(authResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).setContentType("text/html");
        verify(response.getWriter()).write(anyString());
    }

    @Test
    void onAuthenticationSuccess_WhenNewGithubUser_CreatesUserAndRedirects() throws Exception {
        OAuth2User oauthUser = mock(OAuth2User.class);
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("github");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "new@github.com");
        attributes.put("avatar_url", "http://photo.com/avatar.png");
        attributes.put("name", "Github User");
        when(oauthUser.getAttributes()).thenReturn(attributes);

        when(loginService.compruebaCorreo("new@github.com")).thenReturn(0L);
        AuthResponse authResponse = new AuthResponse(true, "Created", new Usuario(), "accessToken", "refreshToken");
        when(usuarioService.createUsuario(any(NewUsuario.class))).thenReturn(authResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(usuarioService).createUsuario(any(NewUsuario.class));
        verify(response).setContentType("text/html");
        verify(response.getWriter()).write(anyString());
    }

    @Test
    void onAuthenticationSuccess_WhenNewGoogleUser_CreatesUserAndRedirects() throws Exception {
        OAuth2User oauthUser = mock(OAuth2User.class);
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "new@google.com");
        attributes.put("picture", "http://photo.com/picture.png");
        when(oauthUser.getAttributes()).thenReturn(attributes);

        when(loginService.compruebaCorreo("new@google.com")).thenReturn(0L);
        AuthResponse authResponse = new AuthResponse(true, "Created", new Usuario(), "accessToken", "refreshToken");
        when(usuarioService.createUsuario(any(NewUsuario.class))).thenReturn(authResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(usuarioService).createUsuario(any(NewUsuario.class));
        verify(response).setContentType("text/html");
        verify(response.getWriter()).write(anyString());
    }
}
