package com.sovereingschool.back_base.Configurations.Filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.Services.LoginService;
import com.sovereingschool.back_base.Services.UsuarioService;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);
    private LoginService loginService;
    private UsuarioService usuarioService;

    private ObjectMapper objectMapper;

    private String front;

    /**
     * Constructor de CustomOAuth2SuccessHandler
     *
     * @param loginService
     * @param usuarioService
     * @param objectMapper
     */
    public CustomOAuth2SuccessHandler(
            @Value("${variable.FRONT}") String front,
            LoginService loginService, UsuarioService usuarioService,
            ObjectMapper objectMapper) {
        this.front = front;
        this.loginService = loginService;
        this.usuarioService = usuarioService;
        this.objectMapper = objectMapper;

    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            logger.error("Tipo de autenticación no compatible.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tipo de autenticación no compatible.");
            return;
        }

        Map<String, Object> attributes = oauthUser.getAttributes();

        String email = (String) attributes.get("email");

        if (email == null || email.isEmpty()) {
            logger.error("No se pudo obtener el correo electrónico.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se pudo obtener el correo electrónico.");
            return;
        }

        AuthResponse authResponse = null;
        Long id = loginService.compruebaCorreo(email);

        if (id == 0L) {
            String registrationId = ((OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId();
            String photo = null;

            if (registrationId.equals("github")) {
                photo = (String) attributes.get("avatar_url");
            } else if (registrationId.equals("google")) {
                photo = (String) attributes.get("picture");
            }

            String name = (String) attributes.getOrDefault("name", "Usuario OAuth2");
            List<String> fotos = photo != null ? List.of(photo) : new ArrayList<>();

            NewUsuario newUser = new NewUsuario(
                    name, email, UUID.randomUUID().toString(),
                    fotos, null, new ArrayList<>(), new Date());

            try {
                authResponse = usuarioService.createUsuario(newUser);
            } catch (RepositoryException | InternalComunicationException e) {
                throw new RuntimeException("Error al crear el usuario: " + e.getMessage());
            }
        } else {
            String password = loginService.getPasswordLogin(id);

            authResponse = loginService.loginUser(id, password);
        }

        if (authResponse == null)
            return;

        String json = objectMapper.writeValueAsString(authResponse);
        String script = "<script>" +
                "window.opener.postMessage(" + json + ", \"" + front + "\");" +
                "window.close();" +
                "</script>";

        response.setContentType("text/html");
        response.getWriter().write(script);
    }
}
