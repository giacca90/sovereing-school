package com.sovereingschool.back_base.Controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sovereingschool.back_base.DTOs.InitApp;
import com.sovereingschool.back_base.Services.InitAppService;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.http.port=0")
public class InitControllerTest {

    @Nested
    class GetInitTests {

        @Test
        public void testGetInit_Success() {
            InitApp mockResponse = mock(InitApp.class);
            String mockToken = "token-secret-123";

            when(initAppService.getInit()).thenReturn(mockResponse);
            when(initAppService.getInitToken()).thenReturn(mockToken);

            given()
                    .header("Authorization", "Bearer " + guestJWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("initToken=" + mockToken));
        }

        @Test
        public void testGetInit_Exception() {
            String mensajeError = "Error interno del servidor";
            when(initAppService.getInit()).thenThrow(new RuntimeException(mensajeError));

            given()
                    .header("Authorization", "Bearer " + guestJWT)
                    .when()
                    .get("")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(equalTo(mensajeError));
        }
    }

    @Nested
    class AuthTests {

        @Test
        public void testAuth_Success() {
            // GIVEN
            String mockInitToken = "auth-token-999";
            when(initAppService.getInitToken()).thenReturn(mockInitToken);

            // 1. Generamos el JWT que el filtro usará para autenticar
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "guestUser", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
            String jwtParaFiltro = jwtUtil.generateToken(auth, "access", 3600000L);

            // WHEN & THEN
            given()
                    .relaxedHTTPSValidation()
                    // 2. IMPORTANTE: Tu filtro busca "refreshToken" para autenticar
                    // Ponemos el JWT aquí para que 'jwtUtil.createAuthenticationFromToken' funcione
                    .cookie("refreshToken", jwtParaFiltro)
                    .when()
                    .get("/auth")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo("OK"))
                    .header("Set-Cookie", containsString("initToken=" + mockInitToken));
        }

        @Test
        public void testAuth_WithoutCookie() {
            // GIVEN - sin enviar cookie pero con JWT en header para autenticación
            String mockInitToken = "auth-token-without-cookie";
            when(initAppService.getInitToken()).thenReturn(mockInitToken);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "guestUser", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
            String jwt = jwtUtil.generateToken(auth, "access", 3600000L);

            // WHEN & THEN - el endpoint debe funcionar sin la cookie (required=false)
            given()
                    .relaxedHTTPSValidation()
                    .header("Authorization", "Bearer " + jwt)
                    .when()
                    .get("/auth")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo("OK"))
                    .header("Set-Cookie", containsString("initToken=" + mockInitToken));
        }

        @Test
        public void testAuth_Exception() {
            // GIVEN
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "guestUser", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
            String jwt = jwtUtil.generateToken(auth, "access", 3600000L);

            when(initAppService.getInitToken()).thenThrow(new RuntimeException("Token generation failed"));

            // WHEN & THEN
            given()
                    .relaxedHTTPSValidation()
                    .header("Authorization", "Bearer " + jwt)
                    .when()
                    .get("/auth")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testAuth_WithUserRole() {
            // GIVEN - usando rol USER en lugar de GUEST
            String mockInitToken = "auth-token-user-role";
            when(initAppService.getInitToken()).thenReturn(mockInitToken);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "regularUser", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            String jwt = jwtUtil.generateToken(auth, "access", 3600000L);

            // WHEN & THEN
            given()
                    .relaxedHTTPSValidation()
                    .header("Authorization", "Bearer " + jwt)
                    .when()
                    .get("/auth")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo("OK"))
                    .header("Set-Cookie", containsString("initToken=" + mockInitToken));
        }
    }

    @Nested
    class DifferentRolesTests {

        @Test
        public void testGetInit_WithUserRole() {
            // GIVEN - usando rol USER
            InitApp mockResponse = mock(InitApp.class);
            String mockToken = "token-user-role-123";

            when(initAppService.getInit()).thenReturn(mockResponse);
            when(initAppService.getInitToken()).thenReturn(mockToken);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            String jwt = jwtUtil.generateToken(auth, "access", 3600000L);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("initToken=" + mockToken));
        }

        @Test
        public void testGetInit_WithProfRole() {
            // GIVEN - usando rol PROF
            InitApp mockResponse = mock(InitApp.class);
            String mockToken = "token-prof-role-456";

            when(initAppService.getInit()).thenReturn(mockResponse);
            when(initAppService.getInitToken()).thenReturn(mockToken);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "profesor", null, List.of(new SimpleGrantedAuthority("ROLE_PROF")));
            String jwt = jwtUtil.generateToken(auth, "access", 3600000L);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("initToken=" + mockToken));
        }

        @Test
        public void testGetInit_WithAdminRole() {
            // GIVEN - usando rol ADMIN
            InitApp mockResponse = mock(InitApp.class);
            String mockToken = "token-admin-role-789";

            when(initAppService.getInit()).thenReturn(mockResponse);
            when(initAppService.getInitToken()).thenReturn(mockToken);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            String jwt = jwtUtil.generateToken(auth, "access", 3600000L);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("initToken=" + mockToken));
        }
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private InitAppService initAppService;

    @Autowired
    private JwtUtil jwtUtil;

    private String guestJWT;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "https://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/init";
        RestAssured.useRelaxedHTTPSValidation();

        // Generamos un JWT de invitado usando el helper específico
        guestJWT = jwtUtil.generateInitToken();
    }
}