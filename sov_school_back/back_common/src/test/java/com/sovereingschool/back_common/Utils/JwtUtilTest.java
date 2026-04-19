package com.sovereingschool.back_common.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Models.Usuario;

/**
 * Tests unitarios para la clase {@link JwtUtil}.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String SECRET = "testSecret";
    private final String ISSUER = "testIssuer";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "privateKay", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "userGenerator", ISSUER);
    }

    /**
     * Prueba que un objeto se convierte correctamente a formato Base64.
     */
    @Test
    void convertirObjetoABase64_DeberiaRetornarBase64Valido() throws IOException {
        Usuario persona = new Usuario();
        persona.setNombreUsuario("pepito");

        String base64 = JwtUtil.convertirObjetoABase64(persona);

        assertNotNull(base64, "El resultado de la conversión no debería ser nulo");
        assertFalse(base64.isBlank(), "El resultado de la conversión no debería estar vacío");

        byte[] decoded = Base64.getDecoder().decode(base64);
        assertTrue(decoded.length > 0, "El contenido decodificado debería tener longitud mayor a cero");
    }

    /**
     * Prueba la generación y validación de un token de acceso estándar.
     */
    @Test
    void generarYValidarToken_DeberiaSerValidoParaUsuario() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        when(auth.getAuthorities()).thenReturn((List) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateToken(auth, "access", 1L);
        assertNotNull(token, "El token generado no debería ser nulo");
        assertTrue(jwtUtil.isTokenValid(token), "El token generado debería ser válido");
        assertEquals("user1", jwtUtil.getUsername(token), "El nombre de usuario en el token debe coincidir");
        assertEquals("ROLE_USER", jwtUtil.getRoles(token), "Los roles en el token deben coincidir");
        assertEquals(1L, jwtUtil.getIdUsuario(token), "El ID de usuario en el token debe coincidir");
    }

    /**
     * Prueba la generación de un token de servidor.
     */
    @Test
    void generarTokenServidor_DeberiaRetornarTokenConRolAdmin() {
        Authentication auth = mock(Authentication.class);
        String token = jwtUtil.generateToken(auth, "server", 0L);

        assertNotNull(token, "El token generado no debería ser nulo");
        assertEquals("server", jwtUtil.getUsername(token), "El nombre de usuario en el token debe ser 'server'");
        assertEquals("ROLE_ADMIN", jwtUtil.getRoles(token), "El rol en el token debe ser 'ROLE_ADMIN'");
    }

    /**
     * Prueba la generación de un token de invitado (visitante).
     */
    @Test
    void generarTokenInvitado_DeberiaRetornarTokenConRolInvitado() {
        String token = jwtUtil.generateInitToken();
        assertNotNull(token, "El token generado no debería ser nulo");
        assertEquals("Visitante", jwtUtil.getUsername(token), "El usuario debería ser 'Visitante'");
        assertEquals("ROLE_GUEST", jwtUtil.getRoles(token), "El rol debería ser 'ROLE_GUEST'");
    }

    /**
     * Prueba la generación de un token para registro de nuevo usuario.
     */
    @Test
    void generarTokenRegistro_DeberiaContenerDatosUsuario() throws Exception {
        NewUsuario newUser = new NewUsuario("test", "test@test.com", "pass", List.of(), null, List.of(), new Date());
        String token = jwtUtil.generateRegistrationToken(newUser);

        assertNotNull(token, "El token de registro no debería ser nulo");
        assertEquals("test@test.com", jwtUtil.getUsername(token), "El correo debe coincidir");
        assertNotNull(jwtUtil.getSpecificClaim(token, "newUsuario"), "El claim 'newUsuario' debería existir");
    }

    @Test
    void testGetAllClaims() {
        String token = jwtUtil.generateInitToken();
        Map<String, Claim> claims = jwtUtil.getAllClaims(token);
        assertNotNull(claims);
        assertTrue(claims.containsKey("rol"));
    }

    @Test
    void testCreateAuthenticationFromToken() {
        Authentication authOrig = mock(Authentication.class);
        when(authOrig.getName()).thenReturn("user1");
        when(authOrig.getAuthorities()).thenReturn((List) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateToken(authOrig, "access", 1L);
        Authentication auth = jwtUtil.createAuthenticationFromToken(token);

        assertNotNull(auth);
        assertEquals("user1", auth.getName());
        assertEquals(1L, auth.getDetails());
    }

    @Test
    void testCreateAuthenticationFromServerToken() {
        String token = jwtUtil.generateToken(null, "server", 0L);
        Authentication auth = jwtUtil.createAuthenticationFromToken(token);
        assertEquals("server", auth.getName());

        Authentication authStatic = jwtUtil.createAuthenticationFromServerToken();
        assertEquals("server", authStatic.getName());
    }

    @Test
    void testDecodeRawToken() {
        String token = jwtUtil.generateInitToken();
        DecodedJWT decoded = jwtUtil.decodeRawToken(token);
        assertNotNull(decoded);
        assertEquals(ISSUER, decoded.getIssuer());
    }

    @Test
    void testExpirationMethods() {
        assertNotNull(jwtUtil.getExpiredForServer());
        assertNotNull(jwtUtil.getExpiredForAccessToken());
        assertNotNull(jwtUtil.getExpiredForRefreshToken());
        assertNotNull(jwtUtil.getExpiredForInitToken());
    }

    @Test
    void testGetSpecificClaim_NotFound() {
        String token = jwtUtil.generateInitToken();
        assertThrows(Exception.class, () -> jwtUtil.getSpecificClaim(token, "nonexistent"));
    }

    @Test
    void testGetRoles_Empty() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        String token = com.auth0.jwt.JWT.create()
                .withIssuer(ISSUER)
                .withSubject("user")
                .withClaim("rol", "")
                .sign(algorithm);
        assertThrows(Exception.class, () -> jwtUtil.getRoles(token));

        String tokenNull = com.auth0.jwt.JWT.create()
                .withIssuer(ISSUER)
                .withSubject("user")
                .sign(algorithm);
        assertThrows(Exception.class, () -> jwtUtil.getRoles(tokenNull));
    }

    @Test
    void testGetUsername_Empty() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        String token = com.auth0.jwt.JWT.create()
                .withIssuer(ISSUER)
                .sign(algorithm);
        assertThrows(Exception.class, () -> jwtUtil.getUsername(token));
    }

    @Test
    void testTokenExpired() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        String token = com.auth0.jwt.JWT.create()
                .withIssuer(ISSUER)
                .withSubject("user")
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .sign(algorithm);
        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> jwtUtil.getUsername(token));
    }

    @Test
    void testInvalidIssuer() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        String token = com.auth0.jwt.JWT.create()
                .withIssuer("wrongIssuer")
                .withSubject("user")
                .sign(algorithm);
        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> jwtUtil.getUsername(token));
    }

    @Test
    void testInvalidTokens() {
        assertThrows(Exception.class, () -> jwtUtil.getUsername(null));
        assertThrows(Exception.class, () -> jwtUtil.getUsername(""));
        assertThrows(Exception.class, () -> jwtUtil.getUsername("invalid.token.here"));
    }
}
