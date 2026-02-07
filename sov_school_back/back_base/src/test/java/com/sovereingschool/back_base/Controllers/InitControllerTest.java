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