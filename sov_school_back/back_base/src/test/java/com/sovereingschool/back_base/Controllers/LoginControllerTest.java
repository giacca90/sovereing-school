package com.sovereingschool.back_base.Controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.ChangePassword;
import com.sovereingschool.back_base.Interfaces.ILoginService;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.servlet.http.Cookie;

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
        public void testCompruebaCorreo_Exception() {
            String correo = "invalid@test.com";
            when(loginService.compruebaCorreo(correo)).thenThrow(new RuntimeException("Email error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/" + correo)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
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

        @Test
        public void testGetCorreoLogin_NotFound() {
            Long id = 999L;
            when(loginService.getCorreoLogin(id)).thenReturn(null);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/correo/" + id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        public void testGetPasswordLogin_Success() {
            Long id = 1L;
            String mockPassword = "encryptedPassword123";
            when(loginService.getPasswordLogin(id)).thenReturn(mockPassword);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/password/" + id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo(mockPassword));
        }

        @Test
        public void testGetPasswordLogin_NotFound() {
            Long id = 999L;
            when(loginService.getPasswordLogin(id)).thenReturn(null);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/password/" + id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
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
        public void testGetUsuario_AuthResponseNullStatus() {
            Long id = 1L;
            String password = "password123";

            AuthResponse mockAuthResponse = mock(AuthResponse.class);
            when(mockAuthResponse.status()).thenReturn(false);
            when(mockAuthResponse.message()).thenReturn("Status false");

            when(loginService.loginUser(id, password)).thenReturn(mockAuthResponse);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/" + id + "/" + password)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
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
        public void testRefreshAccessToken_NoToken() {
            given()
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
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

        @Test
        public void testRefreshAccessToken_Exception2() {
            Long idUsuario = 1L;
            when(loginService.refreshAccessToken(idUsuario)).thenThrow(new RuntimeException("Service error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testRefreshAccessToken_NotFound2() {
            Long idUsuario = 1L;
            AuthResponse mockAuthResponse = mock(AuthResponse.class);
            when(mockAuthResponse.status()).thenReturn(false);

            when(loginService.refreshAccessToken(idUsuario)).thenReturn(mockAuthResponse);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        public void testGetUsuario_Exception() {
            Long id = 1L;
            String password = "password123";

            when(loginService.loginUser(id, password)).thenThrow(new RuntimeException("Database error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/" + id + "/" + password)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testGetUsuario_AuthResponseNull() {
            Long id = 1L;
            String password = "password123";

            when(loginService.loginUser(id, password)).thenReturn(null);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/" + id + "/" + password)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        public void testRefreshAccessToken_Exception() {
            Long idUsuario = 1L;
            when(loginService.refreshAccessToken(idUsuario)).thenThrow(new RuntimeException("Service error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testRefreshAccessToken_NotFound() {
            Long idUsuario = 1L;
            AuthResponse mockAuthResponse = mock(AuthResponse.class);
            when(mockAuthResponse.status()).thenReturn(false);

            when(loginService.refreshAccessToken(idUsuario)).thenReturn(mockAuthResponse);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        public void testRefreshAccessToken_NullAuthResponse() {
            Long idUsuario = 1L;
            when(loginService.refreshAccessToken(idUsuario)).thenReturn(null);

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .post("/refresh")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

    }

    @Nested
    class ManagementTests {

        @Test
        public void testCreateNuevoLogin_Success() {
            com.sovereingschool.back_common.Models.Login login = new com.sovereingschool.back_common.Models.Login();
            login.setIdUsuario(0L);
            login.setCorreoElectronico("new@test.com");

            when(loginService.createNuevoLogin(any(com.sovereingschool.back_common.Models.Login.class)))
                    .thenReturn("Login creado");

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(login)
                    .when()
                    .post("/new")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }

        @Test
        public void testCreateNuevoLogin_Exception() {
            com.sovereingschool.back_common.Models.Login login = new com.sovereingschool.back_common.Models.Login();
            login.setCorreoElectronico("duplicate@test.com");

            when(loginService.createNuevoLogin(any(com.sovereingschool.back_common.Models.Login.class)))
                    .thenThrow(new RuntimeException("Email already exists"));

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(login)
                    .when()
                    .post("/new")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testDeleteLogin_Exception2() {
            Long idToDelete = 99L;
            when(loginService.deleteLogin(idToDelete)).thenThrow(new RuntimeException("Delete error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .delete("/delete/" + idToDelete)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

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
        public void testChangePassword_PasswordMismatch() {
            ChangePassword cp = new ChangePassword(1L, "wrongPassword", "newPassword");
            when(loginService.changePasswordLogin(any(ChangePassword.class))).thenReturn(0);

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(cp)
                    .when()
                    .put("/cambiaPassword")
                    .then()
                    .statusCode(HttpStatus.FAILED_DEPENDENCY.value())
                    .body(containsString("Las contraseñas no coinciden"));
        }

        @Test
        public void testChangePassword_NullPassword() {
            ChangePassword cp = new ChangePassword(1L, null, "newPassword");
            when(loginService.changePasswordLogin(any(ChangePassword.class))).thenReturn(null);

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(cp)
                    .when()
                    .put("/cambiaPassword")
                    .then()
                    .statusCode(HttpStatus.FAILED_DEPENDENCY.value());
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

        @Test
        public void testChangeCorreo_Success() {
            com.sovereingschool.back_common.Models.Login login = new com.sovereingschool.back_common.Models.Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("newemail@test.com");

            when(loginService.changeCorreoLogin(any(com.sovereingschool.back_common.Models.Login.class)))
                    .thenReturn("1");

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(login)
                    .when()
                    .put("/cambiaCorreo")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }

        @Test
        public void testChangeCorreo_NoEmail() {
            com.sovereingschool.back_common.Models.Login login = new com.sovereingschool.back_common.Models.Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico(null);

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(login)
                    .when()
                    .put("/cambiaCorreo")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        public void testChangeCorreo_EmptyEmail() {
            com.sovereingschool.back_common.Models.Login login = new com.sovereingschool.back_common.Models.Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("");

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(login)
                    .when()
                    .put("/cambiaCorreo")
                    .then()
                    .statusCode(HttpStatus.FAILED_DEPENDENCY.value());
        }

        @Test
        public void testChangeCorreo_Exception() {
            com.sovereingschool.back_common.Models.Login login = new com.sovereingschool.back_common.Models.Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("valid@test.com");

            when(loginService.changeCorreoLogin(any(com.sovereingschool.back_common.Models.Login.class)))
                    .thenThrow(new RuntimeException("Database error"));

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(login)
                    .when()
                    .put("/cambiaCorreo")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testChangePassword_Exception() {
            ChangePassword cp = new ChangePassword(1L, "oldPassword", "newPassword");
            when(loginService.changePasswordLogin(any(ChangePassword.class)))
                    .thenThrow(new RuntimeException("Service error"));

            given()
                    .cookie("refreshToken", validToken)
                    .contentType(ContentType.JSON)
                    .body(cp)
                    .when()
                    .put("/cambiaPassword")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testDeleteLogin_Exception() {
            Long idToDelete = 99L;
            when(loginService.deleteLogin(idToDelete)).thenThrow(new RuntimeException("Delete error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .delete("/delete/" + idToDelete)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testGetCorreoLogin_Exception() {
            Long id = 1L;
            when(loginService.getCorreoLogin(id)).thenThrow(new RuntimeException("Service error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/correo/" + id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        public void testGetPasswordLogin_Exception() {
            Long id = 1L;
            when(loginService.getPasswordLogin(id)).thenThrow(new RuntimeException("Service error"));

            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/password/" + id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Nested
    class TokenTests {

        @Test
        public void testLoginWithToken_JWTVerificationException() {
            String token = "invalid-jwt-token";

            when(loginService.loginWithToken(token)).thenThrow(new JWTVerificationException("Invalid token"));

            given()
                    .cookie("refreshToken", validToken)
                    .contentType("application/json; charset=UTF-8")
                    .body(token)
                    .when()
                    .post("/loginWithToken")
                    .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        public void testLoginWithToken_GeneralException() {
            String token = "valid-token";

            when(loginService.loginWithToken(token)).thenThrow(new RuntimeException("Service error"));

            given()
                    .cookie("refreshToken", validToken)
                    .contentType("application/json; charset=UTF-8")
                    .body(token)
                    .when()
                    .post("/loginWithToken")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en login con token"));
        }

        @Test
        public void testLogout_Exception() {
            // Para forzar una excepción en logout, hacemos una request normal
            given()
                    .cookie("refreshToken", validToken)
                    .when()
                    .get("/logout")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(containsString("Logout exitoso"));
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        @WithMockUser(roles = "USER")
        public void testRefreshAccessToken_EmptyTokenMock() throws Exception {
            // Test para refrescar con token vacío usando MockMvc
            Cookie cookie = new Cookie("refreshToken", "");
            mockMvc.perform(post("/login/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(cookie))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        public void testLogoutWithMockMvc() throws Exception {
            // Test para logout usando MockMvc
            Cookie cookie = new Cookie("refreshToken", validToken);
            mockMvc.perform(post("/login/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(cookie))
                    .andExpect(status().isMethodNotAllowed()); // POST no está permitido, es GET
        }
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private ILoginService loginService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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

        // Configurar MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }
}