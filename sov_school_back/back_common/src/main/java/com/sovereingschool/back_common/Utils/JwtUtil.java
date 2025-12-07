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
import com.sovereingschool.back_common.Exceptions.InternalServerException;

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

    public Date getExpiredForServer() {
        return new Date(System.currentTimeMillis() + 60 * 60 * 1000);
    }

    public Date getExpiredForAccessToken() {
        return new Date(System.currentTimeMillis() + 15 * 60 * 1000);
    }

    public Date getExpiredForRefreshToken() {
        return new Date(System.currentTimeMillis() + 15 * 24 * 60 * 60 * 1000);
    }

    public Date getExpiredForInitToken() {
        return new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    /**
     * Función para crear un token JWT
     * 
     * @param authentication Objecto Authentication del usuario
     * @param tokenType      String del tipo de token. Puede ser "server" o "access"
     * @param idUsuario      Long del id del usuario
     * @return String con el token JWT
     */
    public String generateToken(Authentication authentication, String tokenType, Long idUsuario) {

        Algorithm algorithm = Algorithm.HMAC256(this.privateKay);

        String username = tokenType.equals("server") ? "server" : authentication.getName();
        String roles = tokenType.equals("server") ? "ROLE_ADMIN"
                : authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));

        Date expiration = switch (tokenType) {
            case "access" -> getExpiredForAccessToken();
            case "server" -> getExpiredForServer();
            default -> getExpiredForRefreshToken();
        };

        return JWT.create()
                .withIssuer(this.userGenerator)
                .withSubject(username)
                .withClaim("rol", roles)
                .withClaim("idUsuario", idUsuario)
                .withIssuedAt(new Date())
                .withExpiresAt(expiration)
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(System.currentTimeMillis()))
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
                .withNotBefore(new Date(System.currentTimeMillis()))
                .sign(algorithm);
    }

    public String generateRegistrationToken(NewUsuario newUsuario) throws InternalServerException {
        Algorithm algorithm = Algorithm.HMAC256(this.privateKay);
        try {
            String newUserString = convertirObjetoABase64(newUsuario);
            return JWT.create()
                    .withIssuer(this.userGenerator)
                    .withSubject(newUsuario.correoElectronico())
                    .withClaim("rol", "ROLE_USER")
                    .withClaim("newUsuario", newUserString)
                    .withIssuedAt(new Date())
                    .withExpiresAt(getExpiredForInitToken())
                    .withJWTId(UUID.randomUUID().toString())
                    .withNotBefore(new Date(System.currentTimeMillis()))
                    .sign(algorithm);
        } catch (IOException e) {
            throw new InternalServerException("Error al generar el token de registro: " + e.getMessage());
        }
    }

    /**
     * Obtiene el nombre de usuario del token
     * 
     * @param token String con el token JWT
     * @return String con el nombre de usuario
     */

    public String getUsername(String token) throws AuthenticationException {
        DecodedJWT decodedJWT = decodeToken(token);
        String username = decodedJWT.getSubject();

        if (username == null || username.isEmpty()) {
            throw new BadCredentialsException("El token no contiene un nombre de usuario válido");
        }
        return username;
    }

    /**
     * Obtiene los roles del token
     * 
     * @param token String con el token JWT
     * @return String con los roles separados lo ','
     */
    public String getRoles(String token) throws AuthenticationException {
        DecodedJWT decodedJWT = decodeToken(token);
        String roles = decodedJWT.getClaim("rol").asString();

        if (roles == null || roles.isEmpty()) {
            throw new BadCredentialsException("El token no contiene roles válidos");
        }
        return roles;
    }

    /**
     * Obtiene el ID de usuario del token
     * 
     * @param token String con el token JWT
     * @return Long con el ID de usuario
     */
    public Long getIdUsuario(String token) throws AuthenticationException {
        DecodedJWT decodedJWT = decodeToken(token);
        return decodedJWT.getClaim("idUsuario").asLong();
    }

    /**
     * Obtiene un claim específico del token
     * 
     * @param token String con el token JWT
     * @param claim String con el claim que se desea obtener
     * @return String con el valor del claim
     */
    public String getSpecificClaim(String token, String claim) throws AuthenticationException {
        DecodedJWT decodedJWT = decodeToken(token);
        String claimValue = decodedJWT.getClaim(claim).asString();

        if (claimValue == null) {
            throw new BadCredentialsException("El claim solicitado no existe en el token");
        }
        return claimValue;
    }

    /**
     * Obtiene todos los claims del token
     * 
     * @param token String con el token JWT
     * @return Map<String, Claim> con todos los claims del token
     */
    public Map<String, Claim> getAllClaims(String token) throws AuthenticationException {
        DecodedJWT decodedJWT = decodeToken(token);
        return decodedJWT.getClaims();
    }

    /**
     * Crea una Authentication a partir del token
     * 
     * @param token String con el token JWT
     * @return Objecto Authentication del token
     */
    public Authentication createAuthenticationFromToken(String token) throws AuthenticationException {
        DecodedJWT decodedJWT = decodeToken(token);

        String subject = decodedJWT.getSubject();
        String rolesString = decodedJWT.getClaim("rol").asString();

        List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        // 🚫👈 Prevención de recursión infinita para tokens internos del servidor
        if ("server".equals(subject)) {
            return new UsernamePasswordAuthenticationToken("server", null, authorities);
        }

        Long idUsuario = decodedJWT.getClaim("idUsuario").asLong();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(subject, token, authorities);

        auth.setDetails(idUsuario);
        return auth;
    }

    /**
     * Verifica si el token es válido
     * 
     * @param token String con el token JWT
     * @return boolean con el resultado de la verificación
     */

    public boolean isTokenValid(String token) throws AuthenticationException {
        decodeToken(token);
        return true;
    }

    public DecodedJWT decodeRawToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(this.privateKay);
        return JWT.require(algorithm).withIssuer(this.userGenerator).build().verify(token);
    }

    public Authentication createAuthenticationFromServerToken() {
        return new UsernamePasswordAuthenticationToken(
                "server",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
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
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(this.userGenerator)
                    .build();
            return verifier.verify(token);

        } catch (TokenExpiredException ex) {
            throw new InsufficientAuthenticationException("Token expirado: " + ex.getMessage(), ex);

        } catch (JWTVerificationException ex) {
            throw new BadCredentialsException("Error al verificar el token: " + ex.getMessage(), ex);

        } catch (Exception ex) {
            throw new AuthenticationServiceException("Error inesperado al procesar el token: " + ex.getMessage(), ex);
        }
    }

}
