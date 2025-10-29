package com.sovereingschool.back_common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sovereingschool.back_common.DTOs.NewUsuario;

@Component
public class JwtUtil {

    public static String convertirObjetoABase64(Object objeto) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(objeto);
        objStream.flush();

        byte[] bytes = byteStream.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Value("${security.jwt.private.key}")
    private String privateKay;

    @Value("${security.jwt.user.generator}")
    private String userGenerator;

    private Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public Date getExpiredForServer() {
        return new Date(System.currentTimeMillis() + 60 * 60 * 1000); // 1 hour
    }

    public Date getExpiredForAccessToken() {
        return new Date(System.currentTimeMillis() + 15 * 60 * 1000); // 15 minutes
    }

    public Date getExpiredForRefreshToken() {
        return new Date(System.currentTimeMillis() + 15 * 24 * 60 * 60 * 1000); // 15 days
    }

    public Date getExpiredForInitToken() {
        return new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 1 day
    }

    /**
     * Función para crear un token JWT
     * 
     * @param authentication Objecto Authentication del usuario
     * @param tokenType      String del tipo de token. Puede ser "server" o "access"
     * @param id_usuario     Long del id del usuario
     * @return String con el token JWT
     */

    public String generateToken(Authentication authentication, String tokenType, Long id_usuario) {

        Algorithm algorithm = Algorithm.HMAC256(this.privateKay);

        String username = tokenType.equals("server") ? "server" : authentication.getName();
        String roles = tokenType.equals("server") ? "ROLE_ADMIN"
                : authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));

        return JWT.create()
                .withIssuer(this.userGenerator)
                .withSubject(username)
                .withClaim("rol", roles)
                .withClaim("id_usuario", id_usuario)
                .withIssuedAt(new Date())
                .withExpiresAt(tokenType.equals("access") ? getExpiredForAccessToken()
                        : tokenType.equals("server") ? getExpiredForServer() : getExpiredForRefreshToken()) // 1 hour
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(System.currentTimeMillis())) //
                .sign(algorithm);
    }

    /**
     * Función para crear un token de inicio de sesión
     * 
     * @return String con el token JWT
     */

    public String generateInitToken() {
        Algorithm algorithm = Algorithm.HMAC256(this.privateKay);
        return JWT.create()
                .withIssuer(this.userGenerator)
                .withSubject("Visitante")
                .withClaim("rol", "ROLE_GUEST")
                .withIssuedAt(new Date())
                .withExpiresAt(getExpiredForInitToken())
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(System.currentTimeMillis())) //
                .sign(algorithm);
    }

    public String generateRegistrationToken(NewUsuario newUsuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.privateKay);
            return JWT.create()
                    .withIssuer(this.userGenerator)
                    .withSubject(newUsuario.getCorreoElectronico())
                    .withClaim("rol", "ROLE_USER")
                    .withClaim("new_usuario", convertirObjetoABase64(newUsuario))
                    .withIssuedAt(new Date())
                    .withExpiresAt(getExpiredForInitToken())
                    .withJWTId(UUID.randomUUID().toString())
                    .withNotBefore(new Date(System.currentTimeMillis())) //
                    .sign(algorithm);
        } catch (Exception e) {
            logger.error("Error al generar el token de registro: {}", e.getMessage());
            throw new RuntimeException("Error al generar el token de registro: " + e.getMessage());
        }
    }

    /**
     * Obtiene el nombre de usuario del token
     * 
     * @param token String con el token JWT
     * @return String con el nombre de usuario
     */

    public String getUsername(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("El token no puede estar vacío");
        }
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            String username = decodedJWT.getSubject();
            if (username == null || username.isEmpty()) {
                throw new BadCredentialsException("El token no contiene un nombre de usuario válido");
            }
            return username;
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Obtiene los roles del token
     * 
     * @param token String con el token JWT
     * @return String con los roles separados lo ','
     */

    public String getRoles(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("El token no puede estar vacío");
        }
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            String roles = decodedJWT.getClaim("rol").asString();
            if (roles == null || roles.isEmpty()) {
                throw new BadCredentialsException("El token no contiene roles válidos");
            }
            return roles;
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Obtiene el ID de usuario del token
     * 
     * @param token String con el token JWT
     * @return Long con el ID de usuario
     */

    public Long getIdUsuario(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("El token no puede estar vacío");
        }

        try {
            DecodedJWT decodedJWT = decodeToken(token);
            Claim idClaim = decodedJWT.getClaim("id_usuario");
            return idClaim.asLong();
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Obtiene un claim específico del token
     * 
     * @param token String con el token JWT
     * @param claim String con el claim que se desea obtener
     * @return String con el valor del claim
     */

    public String getSpecificClaim(String token, String claim) {
        if (token == null || claim == null || claim.isEmpty()) {
            throw new BadCredentialsException("El JWT decodificado y el claim no pueden ser nulos o vacíos");
        }
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            String claimValue = decodedJWT.getClaim(claim).asString();
            if (claimValue == null) {
                throw new BadCredentialsException("El claim solicitado no existe en el token");
            }
            return claimValue;
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Obtiene todos los claims del token
     * 
     * @param token String con el token JWT
     * @return Map<String, Claim> con todos los claims del token
     */

    public Map<String, Claim> getAllClaims(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("El token no puede estar vacío");
        }
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT.getClaims();
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Crea una Authentication a partir del token
     * 
     * @param token String con el token JWT
     * @return Objecto Authentication del token
     */

    public Authentication createAuthenticationFromToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("El token no puede estar vacío");
        }
        try {
            String username = null;
            if (getIdUsuario(token) != null) {
                username = getIdUsuario(token).toString();
            } else {
                username = getUsername(token);
            }
            String rolesString = getRoles(token);
            Long idUsuario = null;
            if (username != null && !username.equals("server") && !username.equals("Visitante")) {
                idUsuario = getIdUsuario(token);
            }

            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                    .map(SimpleGrantedAuthority::new).toList();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, token,
                    authorities);

            auth.setDetails(idUsuario);

            return auth;
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Verifica si el token es válido
     * 
     * @param token String con el token JWT
     * @return boolean con el resultado de la verificación
     */

    public boolean isTokenValid(String token) {
        try {
            decodeToken(token);
            return true;
        } catch (AuthenticationException e) {
            throw e;
        }
    }

    /**
     * Decodifica y verifica un token JWT
     * 
     * @param token String con el token JWT
     * @return DecodedJWT con el token decodificado
     * @throws AuthenticationException si el token es inválido
     */
    private DecodedJWT decodeToken(String token) throws AuthenticationException {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("El token no puede estar vacío");
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.privateKay);
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(this.userGenerator).build();
            return verifier.verify(token);
        } catch (TokenExpiredException ex) {
            // Token expirado
            throw new InsufficientAuthenticationException("Token expirado: " + ex.getMessage(), ex);
        } catch (JWTVerificationException ex) {
            // Token mal formado, firma inválida, issuer incorrecto, etc.
            throw new BadCredentialsException("Error al verificar el token: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Cualquier otro error inesperado
            throw new AuthenticationServiceException("Error inesperado al procesar el token: " + ex.getMessage(), ex);
        }
    }
}
