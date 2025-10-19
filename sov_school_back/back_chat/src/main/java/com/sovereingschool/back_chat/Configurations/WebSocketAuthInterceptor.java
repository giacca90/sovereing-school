package com.sovereingschool.back_chat.Configurations;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sovereingschool.back_common.Utils.JwtUtil;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    // Inyectamos un proveedor en lugar del bean directo
    public WebSocketAuthInterceptor(JwtUtil jwtUtil, ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        this.jwtUtil = jwtUtil;
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() == StompCommand.CONNECT) {
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            String token = (sessionAttrs != null) ? (String) sessionAttrs.get("token") : null;

            if (token == null || token.isEmpty()) {
                SecurityContextHolder.clearContext();
                logger.error("No hay token en sessionAttributes");
                throw new MessagingException("Falta token en sessionAttributes");
            }
            Long idUsuario = jwtUtil.getIdUsuario(token);
            try {
                Authentication auth = jwtUtil.createAuthenticationFromToken(token);
                Authentication wsAuth = new Authentication() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return auth.getAuthorities();
                    }

                    @Override
                    public Object getCredentials() {
                        return auth.getCredentials();
                    }

                    @Override
                    public Object getDetails() {
                        return idUsuario;
                    }

                    @Override
                    public Object getPrincipal() {
                        return auth.getPrincipal();
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return auth.isAuthenticated();
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
                        auth.setAuthenticated(isAuthenticated);
                    }

                    @Override
                    public String getName() {
                        return idUsuario.toString();
                    }
                };
                SecurityContextHolder.getContext().setAuthentication(wsAuth);
                accessor.setUser(wsAuth);
                return message;
            } catch (AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                logger.error("Error en el token de acceso: {}", ex.getMessage());
                String destination = accessor.getDestination(); // Ej: "/app/init"
                if (destination != null) {
                    // Obtén el SimpMessagingTemplate solo cuando lo necesites
                    SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
                    if (messagingTemplate != null) {
                        messagingTemplate.convertAndSendToUser(idUsuario.toString(), destination,
                                "Token inválido: " + ex.getMessage());
                        return null;
                    } else {
                        logger.error("SimpMessagingTemplate no disponible en este momento");
                        throw new MessagingException("Token inválido y no hay messagingTemplate");
                    }
                } else {
                    logger.error("No hay destino para el mensaje de refresh");
                    throw new MessagingException("Token inválido y sin destino para enviar el mensaje");
                }
            }
        } else {
            if (accessor.getUser() instanceof Authentication auth) {
                SecurityContextHolder.getContext().setAuthentication(auth);
                return message;
            }
            throw new MessagingException("No hay autenticación en el WebSocket");
        }
    }

    @Override
    public void afterSendCompletion(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent,
            @Nullable Exception ex) {
        if (ex != null) {
            logger.error("WebSocketAuthInterceptor: {}", ex.getMessage());
        }
        SecurityContextHolder.clearContext();
    }
}
