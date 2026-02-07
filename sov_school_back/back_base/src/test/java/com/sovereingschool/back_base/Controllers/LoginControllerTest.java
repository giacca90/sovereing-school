package com.sovereingschool.back_base.Controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
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

import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.ChangePassword;
import com.sovereingschool.back_base.Interfaces.ILoginService;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.http.port=0")
public class LoginControllerTest {

    @Nested
    class BasicGetTests {

        @Test
        public void testCompruebaCorreo_Success() {
            String correo = "test@test.com";
            Long mockId = 1L;

            // Configuramos el mock para devolver el Long que mencionas
            when(loginService.compruebaCorreo(correo)).thenReturn(mockId);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/" + correo)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    // Verificamos que el body contiene el número (como string en el body HTTP)
                    .body(equalTo(mockId.toString()));
        }

        @Test
        public void testGetCorreoLogin_Success() {
            Long id = 1L;
            String mockCorreo = "user@mail.com";
            when(loginService.getCorreoLogin(id)).thenReturn(mockCorreo);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/correo/" + id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo(mockCorreo));
        }
    }

    @Nested
    class AuthenticationFlowTests {

        @Test
        public void testGetUsuario_LoginSuccess() {
            Long id = 1L;
            String password = "password123";
            String newRefreshToken = "new-refresh-token-xyz";

            AuthResponse mockAuthResponse = mock(AuthResponse.class);
            when(mockAuthResponse.status()).thenReturn(true);
            when(mockAuthResponse.refreshToken()).thenReturn(newRefreshToken);
            when(mockAuthResponse.message()).thenReturn("Login OK");
            when(mockAuthResponse.usuario()).thenReturn(new Usuario());
            when(mockAuthResponse.accessToken()).thenReturn("access-token-xyz");

            when(loginService.loginUser(id, password)).thenReturn(mockAuthResponse);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/" + id + "/" + password)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("refreshToken=" + newRefreshToken))
                    .header("Set-Cookie", containsString("HttpOnly"))
                    .header("Set-Cookie", containsString("Secure"));
        }

        @Test
        public void testRefreshAccessToken_Success() {
            Long idUsuario = 1L;
            AuthResponse mockAuthResponse = mock(AuthResponse.class);
            when(mockAuthResponse.status()).thenReturn(true);
            when(mockAuthResponse.refreshToken()).thenReturn("refreshed-123");
            when(mockAuthResponse.message()).thenReturn("Refreshed");

            when(loginService.refreshAccessToken(idUsuario)).thenReturn(mockAuthResponse);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("refreshToken=refreshed-123"));
        }

        @Test
        public void testLogout_Success() {
            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/logout")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("Max-Age=0"));
        }
    }

    @Nested
    class ManagementTests {

        @Test
        public void testChangePassword_Success() {
            ChangePassword cp = new ChangePassword(1L, "oldPassword", "newPassword");
            when(loginService.changePasswordLogin(any(ChangePassword.class))).thenReturn(1);

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(cp)
                    .when()
                    .put("/cambiaPassword")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(containsString("Contraseña cambiada con éxito"));
        }

        @Test
        public void testDeleteLogin_Success() {
            Long idToDelete = 99L;
            when(loginService.deleteLogin(idToDelete)).thenReturn("Eliminado");

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .delete("/delete/" + idToDelete)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo("Eliminado"));
        }
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private ILoginService loginService;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "https://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/login";
        RestAssured.useRelaxedHTTPSValidation();

        // Token para pasar el JwtTokenCookieFilter (idUsuario 1L)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testUser", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        validToken = jwtUtil.generateToken(auth, "refresh", 1L);
    }
}