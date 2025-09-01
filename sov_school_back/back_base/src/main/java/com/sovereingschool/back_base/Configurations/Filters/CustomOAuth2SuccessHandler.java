package com.sovereingschool.back_base.Configurations.Filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private LoginService loginService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${variable.FRONT}")
    private String front;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            System.err.println("Tipo de autenticación no compatible.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tipo de autenticación no compatible.");
            return;
        }

        Map<String, Object> attributes = oauthUser.getAttributes();

        String email = (String) attributes.get("email");

        if (email == null || email.isEmpty()) {
            System.err.println("No se pudo obtener el correo electrónico.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se pudo obtener el correo electrónico.");
            return;
        }

        AuthResponse authResponse;
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

            authResponse = usuarioService.createUsuario(newUser);
        } else {
            String password = loginService.getPasswordLogin(id);

            authResponse = loginService.loginUser(id, password);
        }

        String json = objectMapper.writeValueAsString(authResponse);
        String script = "<script>" +
                "window.opener.postMessage(" + json + ", \"" + front + "\");" +
                "window.close();" +
                "</script>";

        response.setContentType("text/html");
        response.getWriter().write(script);
    }
}
