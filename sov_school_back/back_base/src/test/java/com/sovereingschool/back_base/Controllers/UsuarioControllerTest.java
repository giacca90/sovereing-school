package com.sovereingschool.back_base.Controllers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.CursosUsuario;
import com.sovereingschool.back_base.Services.UsuarioService;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityNotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.http.port=0")
@TestPropertySource(properties = "variable.FOTOS_DIR=.")
@DisplayName("UsuarioController Unit Tests with RestAssured")
public class UsuarioControllerTest {

	@Nested
	@DisplayName("GET /usuario/{id}")
	class GetUsuarioTests {

		@Test
		@DisplayName("Should return usuario when found")
		public void testGetUsuario_Success() throws Exception {
			Long id = 1L;
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(id);
			usuario.setNombreUsuario("Test User");
			when(usuarioService.getUsuario(id)).thenReturn(usuario);

			given().cookie("refreshToken", validToken).when().get("/usuario/" + id).then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testGetUsuario_NotFound() throws Exception {
			Long id = 99L;
			when(usuarioService.getUsuario(id))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/" + id).then()
					.statusCode(HttpStatus.NOT_FOUND.value());
		}

		@Test
		@DisplayName("Should return 500 on internal server error")
		public void testGetUsuario_InternalError() throws Exception {
			Long id = 1L;
			when(usuarioService.getUsuario(id)).thenThrow(new RuntimeException("Database error"));

			given().cookie("refreshToken", validToken).when().get("/usuario/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	@Nested
	@DisplayName("GET /usuario/nombre/{id}")
	class GetNombreTests {

		@Test
		@DisplayName("Should return nombre when usuario exists")
		public void testGetNombreUsuario_Success() throws Exception {
			Long id = 1L;
			when(usuarioService.getNombreUsuario(id)).thenReturn("Test User");

			given().cookie("refreshToken", validToken).when()
					.get("/usuario/nombre/" + id).then()
					.statusCode(HttpStatus.OK.value())
					.and()
					.body(containsString("Test User"));
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testGetNombreUsuario_NotFound() throws Exception {
			Long id = 99L;
			when(usuarioService.getNombreUsuario(id))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when()
					.get("/usuario/nombre/" + id).then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testGetNombreUsuario_BadRequest() throws Exception {
			Long id = 99L;
			when(usuarioService.getNombreUsuario(id))
					.thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when()
					.get("/usuario/nombre/" + id).then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 on runtime exception")
		public void testGetNombreUsuario_RuntimeError() throws Exception {
			Long id = 1L;
			when(usuarioService.getNombreUsuario(id)).thenThrow(new RuntimeException("Unexpected error"));

			given().cookie("refreshToken", validToken).when()
					.get("/usuario/nombre/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Unexpected error"));
		}

		@Test
		@DisplayName("Should return 500 on unknown exception")
		public void testGetNombreUsuario_UnknownError() throws Exception {
			Long id = 1L;
			doThrow(new Exception("Unexpected error"))
					.when(usuarioService).getNombreUsuario(id);
			given().cookie("refreshToken", validToken).when().get("/usuario/nombre/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en encontrar el nombre del usuario"));
		}
	}

	@Nested
	@DisplayName("GET /usuario/roll/{id}")
	class GetRollTests {

		@Test
		@DisplayName("Should return roll when usuario exists")
		public void testGetRollUsuario_Success() throws Exception {
			Long id = 1L;
			when(usuarioService.getRollUsuario(id)).thenReturn(RoleEnum.USER);

			given().cookie("refreshToken", validToken).when().get("/usuario/roll/" + id).then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testGetRollUsuario_NotFound() throws Exception {
			Long id = 99L;
			when(usuarioService.getRollUsuario(id))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/roll/" + id).then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 400 when bad request")
		public void testGetRollUsuario_BadRequest() throws Exception {
			Long id = 99L;
			when(usuarioService.getRollUsuario(id))
					.thenThrow(new IllegalArgumentException("Argumento no valido"));

			given().cookie("refreshToken", validToken).when().get("/usuario/roll/" + id).then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.body(containsString("Argumento no valido"));
		}

		@Test
		@DisplayName("Should return 500 when server error")
		public void testGetRollUsuario_RuntimeError() throws Exception {
			Long id = 99L;
			when(usuarioService.getRollUsuario(id))
					.thenThrow(new RuntimeException("Error en el servidor"));

			given().cookie("refreshToken", validToken).when().get("/usuario/roll/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en el servidor"));
		}

		@Test
		@DisplayName("Should return 500 when server error")
		public void testGetRollUsuario_UnknownError() throws Exception {
			Long id = 99L;
			when(usuarioService.getRollUsuario(id))
					.thenThrow(new Exception("Error en el servidor"));

			given().cookie("refreshToken", validToken).when().get("/usuario/roll/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en obtener el roll del usuario: "));
		}
	}

	@Nested
	@DisplayName("GET /usuario/plan/{id}")
	class GetPlanTests {

		@Test
		@DisplayName("Should return plan when usuario exists")
		public void testGetPlanUsuario_Success() throws Exception {
			Long id = 1L;
			Plan plan = new Plan();
			plan.setIdPlan(1L);
			plan.setNombrePlan("Premium");
			when(usuarioService.getPlanUsuario(id)).thenReturn(plan);

			given().cookie("refreshToken", validToken).when().get("/usuario/plan/" + id).then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testGetPlanUsuario_NotFound() throws Exception {
			Long id = 99L;
			when(usuarioService.getPlanUsuario(id))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/plan/" + id).then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 400 when bad request")
		public void testGetPlanUsuario_BadRequest() throws Exception {
			Long id = 99L;
			when(usuarioService.getPlanUsuario(id))
					.thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/plan/" + id).then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 when runtime error")
		public void testGetPlanUsuario_RuntimeError() throws Exception {
			Long id = 99L;
			when(usuarioService.getPlanUsuario(id))
					.thenThrow(new RuntimeException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/plan/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 when runtime error")
		public void testGetPlanUsuario_UnknownError() throws Exception {
			Long id = 99L;
			when(usuarioService.getPlanUsuario(id))
					.thenThrow(new Exception("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/plan/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en obtener el plan del usuario: "));
		}
	}

	@Nested
	@DisplayName("GET /usuario/cursos/{id}")
	class GetCursosTests {

		@Test
		@DisplayName("Should return cursos when usuario exists")
		public void testGetCursosUsuario_Success() throws Exception {
			Long id = 1L;
			Curso curso = new Curso();
			curso.setIdCurso(1L);
			curso.setNombreCurso("Java Básico");
			when(usuarioService.getCursosUsuario(id)).thenReturn(List.of(curso));

			given().cookie("refreshToken", validToken).when().get("/usuario/cursos/" + id).then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return empty list when no cursos")
		public void testGetCursosUsuario_Empty() throws Exception {
			Long id = 1L;
			when(usuarioService.getCursosUsuario(id)).thenReturn(new ArrayList<>());

			given().cookie("refreshToken", validToken).when().get("/usuario/cursos/" + id).then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testGetCursosUsuario_NotFound() throws Exception {
			Long id = 99L;
			when(usuarioService.getCursosUsuario(id))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/cursos/" + id).then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.body(containsString("No existe el usuario"));
		}

		@Test
		@DisplayName("Should return 400 when bad request")
		public void testGetCursosUsuario_BadRequest() throws Exception {
			Long id = 99L;
			when(usuarioService.getCursosUsuario(id))
					.thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/cursos/" + id).then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 when runtime error")
		public void testGetCursosUsuario_RuntimeError() throws Exception {
			Long id = 99L;
			when(usuarioService.getCursosUsuario(id))
					.thenThrow(new RuntimeException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/cursos/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 when unknown error")
		public void testGetCursosUsuario_UnknownError() throws Exception {
			Long id = 99L;
			when(usuarioService.getCursosUsuario(id))
					.thenThrow(new Exception("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().get("/usuario/cursos/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en obtener los cursos del usuario: "));
		}
	}

	@Nested
	@DisplayName("GET /usuario/profes")
	class GetProfesTests {

		@Test
		@DisplayName("Should return list of profes successfully")
		public void testGetProfes_Success() throws Exception {
			Usuario prof = new Usuario();
			prof.setIdUsuario(2L);
			prof.setNombreUsuario("Prof Juan");
			when(usuarioService.getProfes()).thenReturn(List.of(prof));

			given().cookie("refreshToken", validToken).when().get("/usuario/profes").then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return empty list when no profes")
		public void testGetProfes_Empty() throws Exception {
			when(usuarioService.getProfes()).thenReturn(new ArrayList<>());

			given().cookie("refreshToken", validToken).when().get("/usuario/profes").then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should error when have a error")
		public void testGetProfes_Error() throws Exception {
			when(usuarioService.getProfes()).thenThrow(Exception.class);

			given().cookie("refreshToken", validToken).when().get("/usuario/profes").then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	@Nested
	@DisplayName("GET /usuario/getAll")
	class GetAllTests {

		@Test
		@DisplayName("Should return all usuarios successfully")
		public void testGetAllUsuarios_Success() throws RepositoryException, Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			usuario.setNombreUsuario("Usuario 1");
			when(usuarioService.getAllUsuarios()).thenReturn(List.of(usuario));

			given().cookie("refreshToken", adminToken).when().get("/usuario/getAll").then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return empty list when no usuarios")
		public void testGetAllUsuarios_Empty() throws RepositoryException, Exception {
			when(usuarioService.getAllUsuarios()).thenReturn(new ArrayList<>());

			given().cookie("refreshToken", adminToken).when().get("/usuario/getAll").then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return Unautorized when user is not admin")
		public void testGetAllUsuarios_Unautorized() throws RepositoryException, Exception {
			when(usuarioService.getAllUsuarios()).thenThrow(new RuntimeException("Database error"));

			given()
					.cookie("refreshToken", validToken)
					.when()
					.get("/usuario/getAll")
					.then()
					.statusCode(HttpStatus.FORBIDDEN.value());
		}

		@Test
		@DisplayName("Should return RuntimeError when runtime error")
		public void testGetAllUsuarios_RuntimeError() throws RepositoryException, Exception {
			when(usuarioService.getAllUsuarios()).thenThrow(new RuntimeException("Database error"));

			given()
					.cookie("refreshToken", adminToken)
					.when()
					.get("/usuario/getAll")
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Database error"));
		}

		@Test
		@DisplayName("Should return UnknownError when unknown error")
		public void testGetAllUsuarios_UnknownError() throws RepositoryException, Exception {
			when(usuarioService.getAllUsuarios()).thenThrow(new Exception("Database error"));

			given()
					.cookie("refreshToken", adminToken)
					.when()
					.get("/usuario/getAll")
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en obtener todos los usuarios:"));
		}
	}

	@Nested
	@DisplayName("POST /usuario/nuevo")
	class CreateUsuarioTests {

		NewUsuario newUsuario = new NewUsuario("Juan", "juan@example.com", "password123",
				new ArrayList<>(), null,
				new ArrayList<>(), new Date());

		@Test
		@DisplayName("Should successfully create a new user")
		public void testCreateUsuario_Success() throws RepositoryException, InternalComunicationException {

			AuthResponse response = new AuthResponse(true, "Usuario creado exitosamente", null, "token",
					"refreshToken");
			when(usuarioService.createUsuario(any())).thenReturn(response);

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(newUsuario).when()
					.post("/usuario/nuevo").then().statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 200 on invalid data")
		public void testCreateUsuario_BadRequest() throws Exception {
			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(newUsuario)
					.when()
					.post("/usuario/nuevo")
					.then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 403 on AccessDeniedException")
		public void testCreateUsuario_AccessDenied() throws Exception {
			when(usuarioService.sendConfirmationEmail(any())).thenThrow(new AccessDeniedException("Acceso denegado"));

			given().cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(newUsuario)
					.when()
					.post("/usuario/nuevo")
					.then()
					.statusCode(HttpStatus.FORBIDDEN.value())
					.body(containsString("Acceso denegado"));
		}

		@Test
		@DisplayName("Should return 404 on EntityNotFoundException")
		public void testCreateUsuario_NotFound() throws Exception {
			when(usuarioService.sendConfirmationEmail(any())).thenThrow(new EntityNotFoundException("No encontrado"));

			given().cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(newUsuario)
					.when()
					.post("/usuario/nuevo")
					.then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.body(containsString("No encontrado"));
		}

		@Test
		@DisplayName("Should return 400 on IllegalArgumentException")
		public void testCreateUsuario_IllegalArgument() throws Exception {
			when(usuarioService.sendConfirmationEmail(any()))
					.thenThrow(new IllegalArgumentException("Datos inválidos"));

			given().cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(newUsuario)
					.when()
					.post("/usuario/nuevo")
					.then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.body(containsString("Datos inválidos"));
		}

		@Test
		@DisplayName("Should return 500 on RuntimeException")
		public void testCreateUsuario_RuntimeError() throws Exception {
			when(usuarioService.sendConfirmationEmail(any())).thenThrow(new RuntimeException("Error de ejecución"));

			given().cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(newUsuario)
					.when()
					.post("/usuario/nuevo")
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.body(containsString("Error de ejecución"));
		}

		@Test
		@DisplayName("Should return 500 on generic Exception and clean SVG")
		public void testCreateUsuario_GenericException() throws Exception {
			// Usamos thenAnswer para forzar la Exception genérica y cubrir el último catch
			when(usuarioService.sendConfirmationEmail(any())).thenAnswer(invocation -> {
				throw new Exception("Error inesperado");
			});

			try {
				given().cookie("refreshToken", validToken)
						.contentType(ContentType.JSON)
						.body(newUsuario)
						.when()
						.post("/usuario/nuevo")
						.then()
						.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.body(containsString("Error en crear el nuevo usuario: Error inesperado"));
			} finally {
				// Borrar imagen que contenga unit-test.svg al final del test
				borrarArchivosTemporalesSvg();
			}
		}

		private void borrarArchivosTemporalesSvg() {
			File folder = new File(".");
			File[] files = folder.listFiles((dir, name) -> name != null && name.endsWith("unit-test.svg"));
			if (files != null) {
				for (File f : files) {
					f.delete();
				}
			}
		}

	}

	@Nested
	@DisplayName("POST /usuario/confirmation")
	@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
	class ConfirmationTests {

		private final String ENDPOINT = "/usuario/confirmation";

		@MockitoBean
		private JwtUtil jwtUtil;

		@Test
		@DisplayName("Success: Should return 200 and set secure cookie")
		public void testConfirmation_Success() throws Exception {
			String token = "valid_token";
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			AuthResponse mockAuth = new AuthResponse(true, "OK", usuario, "at", "rt_secret");

			when(jwtUtil.isTokenValid(token)).thenReturn(true);
			NewUsuario newU = new NewUsuario("userTest", "a@b.c", "pass", List.of(), null, List.of(), new Date());
			String b64 = toBase64(newU);
			when(jwtUtil.getSpecificClaim(anyString(), eq("new_usuario"))).thenReturn(b64);
			when(usuarioService.createUsuario(any())).thenReturn(mockAuth);

			given()
					.contentType(ContentType.TEXT)
					.body(token)
					.when()
					.post(ENDPOINT)
					.then()
					.statusCode(200)
					.header("Set-Cookie", containsString("refreshToken=rt_secret"))
					.body("refreshToken", nullValue());
		}

		@Test
		@DisplayName("Unauthorized: Should return 401 when claim containsString missing")
		public void testConfirmation_NoClaim() throws Exception {
			when(jwtUtil.isTokenValid(anyString())).thenReturn(true);
			when(jwtUtil.getSpecificClaim(anyString(), eq("new_usuario"))).thenReturn("");

			given()
					.body("token_sin_claim")
					.when()
					.post(ENDPOINT)
					.then()
					.statusCode(401)
					.body(containsString("Error en el token de acceso"));
		}

		@Test
		@DisplayName("Conflict: Should return 400 on DataIntegrityViolationException")
		public void testConfirmation_DataIntegrity() throws Exception {
			when(jwtUtil.isTokenValid(anyString())).thenReturn(true);
			NewUsuario newU = new NewUsuario("userTest", "a@b.c", "pass", List.of(), null, List.of(), new Date());
			when(jwtUtil.getSpecificClaim(anyString(), anyString())).thenReturn(toBase64(newU));
			when(usuarioService.createUsuario(any())).thenThrow(new DataIntegrityViolationException("Conflict"));

			given()
					.body("token")
					.when()
					.post(ENDPOINT)
					.then()
					.statusCode(400)
					.body(containsString("El usuario ya existe"));
		}

		@Test
		@DisplayName("Forbidden: Should handle AccessDeniedException")
		public void testConfirmation_AccessDenied() throws Exception {
			// Para forzar excepciones en métodos void: doThrow
			doThrow(new AccessDeniedException("Prohibido"))
					.when(jwtUtil).isTokenValid(anyString());

			given()
					.body("token")
					.when()
					.post(ENDPOINT)
					.then()
					.statusCode(403)
					.body(containsString("Prohibido"));
		}

		@Test
		@DisplayName("Runtime: Should handle RuntimeException")
		public void testConfirmation_Runtime() throws Exception {
			doThrow(new RuntimeException("Bug interno"))
					.when(jwtUtil).isTokenValid(anyString());

			given()
					.body("token")
					.when()
					.post(ENDPOINT)
					.then()
					.statusCode(500)
					.body(containsString("Bug interno"));
		}

		@Test
		@DisplayName("Coverage: Should return 500 on generic Exception")
		public void testConfirmation_GenericException() throws Exception {
			// Usamos doAnswer para saltar restricciones de Checked Exceptions
			doAnswer(inv -> {
				throw new Exception("Error fatal");
			}).when(jwtUtil).isTokenValid(anyString());

			given()
					.body("token")
					.when()
					.post(ENDPOINT)
					.then()
					.statusCode(500)
					.body(containsString("Error en crear el usuario: Error fatal"));
		}

		@AfterEach
		void tearDown() {
			borrarArchivosTemporalesSvg();
		}

		private String toBase64(NewUsuario newU) throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				oos.writeObject(newU);
			}
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}

		private void borrarArchivosTemporalesSvg() {
			File folder = new File(".");
			File[] files = folder.listFiles((dir, name) -> name != null && name.endsWith("unit-test.svg"));
			if (files != null) {
				for (File f : files) {
					f.delete();
				}
			}
		}
	}

	@Nested
	@DisplayName("PUT /usuario/edit")
	class EditTests {

		@Test
		@DisplayName("Should edit usuario successfully")
		public void testEditUsuario_Success() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			usuario.setNombreUsuario("Juan Actualizado");
			when(usuarioService.updateUsuario(any())).thenReturn(usuario);

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(usuario)
					.when()
					.put("/usuario/edit")
					.then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 401 on Different user")
		public void testEditUsuario_NoAuthentication() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(10L);
			when(usuarioService.updateUsuario(any())).thenReturn(usuario);
			Authentication auth = new UsernamePasswordAuthenticationToken("testUser", null,
					List.of(new SimpleGrantedAuthority("ROLE_USER")));

			String token = jwtUtil.generateToken(auth, "access", 1L);

			given()
					.cookie("refreshToken", token)
					.contentType(ContentType.JSON).body(usuario)
					.when()
					.put("/usuario/edit")
					.then()
					.statusCode(HttpStatus.UNAUTHORIZED.value())
					.and()
					.assertThat()
					.body(containsString("Error en el token de acceso"));
		}

		@Test
		@DisplayName("Should return 400 on illegal argument")
		public void testEditUsuario_BadRequest() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.updateUsuario(any())).thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/edit").then().statusCode(HttpStatus.BAD_REQUEST.value());
		}

		@Test
		@DisplayName("Should return 404 on Entity NotFound")
		public void testEditUsuario_EntityNotFound() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.updateUsuario(any())).thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/edit").then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.assertThat().body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 400 on illegal argument")
		public void testEditUsuario_IllegalArgument() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.updateUsuario(any())).thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/edit").then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.assertThat().body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 on internal error")
		public void testEditUsuario_InternalError() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.updateUsuario(any())).thenThrow(new RuntimeException("Database error"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/edit").then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		@Test
		@DisplayName("Should return 500 on internal error")
		public void testEditUsuario_Error() throws InternalServerException {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.updateUsuario(any())).thenThrow(new Exception("Database error"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/edit").then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.assertThat()
					.body(containsString("Error en editar el usuario:"));
		}

		@Test
		@DisplayName("Should return 401 when no refreshToken cookie is provided")
		public void testEditUsuario_AuthenticationNull_NoCookie() {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);

			given().contentType(ContentType.JSON).body(usuario).when().put("/usuario/edit").then()
					.statusCode(HttpStatus.UNAUTHORIZED.value());
		}
	}

	@Nested
	@DisplayName("PUT /usuario/plan")
	class ChangePlanTests {

		@Test
		@DisplayName("Should change usuario plan successfully")
		public void testChangePlanUsuario_Success() throws Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			Plan plan = new Plan();
			plan.setIdPlan(2L);
			when(usuarioService.changePlanUsuario(any())).thenReturn(1);

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/plan").then().statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 401 when usuario is invalid")
		public void testChangePlanUsuario_Invalid() throws Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(10L);
			when(usuarioService.changePlanUsuario(any()))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON).body(usuario)
					.when()
					.put("/usuario/plan")
					.then()
					.statusCode(HttpStatus.UNAUTHORIZED.value())
					.and()
					.body(containsString("Error en el token de acceso"));
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testChangePlanUsuario_NotFound() throws Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.changePlanUsuario(any()))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON).body(usuario)
					.when()
					.put("/usuario/plan")
					.then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 400 when bad request")
		public void testChangePlanUsuario_IllegalArgument() throws Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.changePlanUsuario(any()))
					.thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(usuario)
					.when()
					.put("/usuario/plan")
					.then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 when runtime error")
		public void testChangePlanUsuario_RuntimeError() throws Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.changePlanUsuario(any()))
					.thenThrow(new RuntimeException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(usuario).when()
					.put("/usuario/plan").then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		@Test
		@DisplayName("Should return 500 when unknown error")
		public void testChangePlanUsuario_UnknownError() throws Exception {
			Usuario usuario = new Usuario();
			usuario.setIdUsuario(1L);
			when(usuarioService.changePlanUsuario(any()))
					.thenThrow(new Exception("Usuario no encontrado"));

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(usuario)
					.when()
					.put("/usuario/plan")
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en cambiar el plan del usuario:"));
		}
	}

	@Nested
	@DisplayName("PUT /usuario/cursos")
	class ChangeCursosTests {

		@Test
		@DisplayName("Should update usuario courses successfully")
		public void testChangeCursosUsuario_Success() throws RepositoryException {
			CursosUsuario cursosUsuario = new CursosUsuario(1L, List.of(1L));
			when(usuarioService.changeCursosUsuario(any())).thenReturn(1);

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON)
					.body(cursosUsuario)
					.when()
					.put("/usuario/cursos")
					.then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 401 when usuario is invalid")
		public void testChangeCursosUsuario_Invalid() throws RepositoryException {
			CursosUsuario cursosUsuario = new CursosUsuario(99L, new ArrayList<>());

			given()
					.cookie("refreshToken", validToken)
					.contentType(ContentType.JSON).body(cursosUsuario)
					.when()
					.put("/usuario/cursos")
					.then()
					.statusCode(HttpStatus.UNAUTHORIZED.value())
					.and()
					.body(containsString("Error en el token de acceso"));
		}

		@Test
		@DisplayName("Should return 401 when usuario not found")
		public void testChangeCursosUsuario_NotFound() throws RepositoryException {
			CursosUsuario cursosUsuario = new CursosUsuario(1L, new ArrayList<>());
			when(usuarioService.changeCursosUsuario(any()))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(cursosUsuario)
					.when()
					.put("/usuario/cursos")
					.then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 400 when bad request")
		public void testChangeCursosUsuario_IllegalArgument() throws RepositoryException {
			CursosUsuario cursosUsuario = new CursosUsuario(1L, new ArrayList<>());
			when(usuarioService.changeCursosUsuario(any()))
					.thenThrow(new IllegalArgumentException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(cursosUsuario)
					.when()
					.put("/usuario/cursos")
					.then()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 500 when runtime error")
		public void testChangeCursosUsuario_RuntimeError() throws RepositoryException {
			CursosUsuario cursosUsuario = new CursosUsuario(1L, new ArrayList<>());
			when(usuarioService.changeCursosUsuario(any()))
					.thenThrow(new RuntimeException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(cursosUsuario)
					.when()
					.put("/usuario/cursos")
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}

		@Test
		@DisplayName("Should return 401 when usuario not found")
		public void testChangeCursosUsuario_UnknownError() throws RepositoryException {
			CursosUsuario cursosUsuario = new CursosUsuario(1L, new ArrayList<>());
			when(usuarioService.changeCursosUsuario(any()))
					.thenThrow(new Exception("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).contentType(ContentType.JSON).body(cursosUsuario)
					.when()
					.put("/usuario/cursos")
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Usuario no encontrado"));
		}
	}

	@Nested
	@DisplayName("DELETE /usuario/delete/{id}")
	class DeleteTests {

		@Test
		@DisplayName("Should delete usuario successfully")
		public void testDeleteUsuario_Success() throws RepositoryException, InternalComunicationException {
			Long id = 1L;
			when(usuarioService.deleteUsuario(id)).thenReturn("Usuario eliminado correctamente");

			given().cookie("refreshToken", validToken).when().delete("/usuario/delete/" + id).then()
					.statusCode(HttpStatus.OK.value());
		}

		@Test
		@DisplayName("Should return 404 when usuario not found")
		public void testDeleteUsuario_NotFound() throws RepositoryException, InternalComunicationException {
			Long id = 99L;
			when(usuarioService.deleteUsuario(id))
					.thenThrow(new EntityNotFoundException("Usuario no encontrado"));

			given().cookie("refreshToken", validToken).when().delete("/usuario/delete/" + id).then()
					.statusCode(HttpStatus.NOT_FOUND.value());
		}

		@Test
		@DisplayName("Should return 500 on internal error")
		public void testDeleteUsuario_IllegalArgument()
				throws RepositoryException, InternalComunicationException {
			Long id = 1L;
			when(usuarioService.deleteUsuario(id)).thenThrow(new IllegalArgumentException("Database error"));

			given()
					.cookie("refreshToken", validToken)
					.when()
					.delete("/usuario/delete/" + id)
					.then()
					.statusCode(HttpStatus.BAD_REQUEST.value());
		}

		@Test
		@DisplayName("Should return 500 on internal error")
		public void testDeleteUsuario_InternalError()
				throws RepositoryException, InternalComunicationException {
			Long id = 1L;
			when(usuarioService.deleteUsuario(id)).thenThrow(new RuntimeException("Database error"));

			given().cookie("refreshToken", validToken).when().delete("/usuario/delete/" + id).then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		@Test
		@DisplayName("Should return 500 on unknown error")
		public void testDeleteUsuario_UnknownError()
				throws RepositoryException, InternalComunicationException {
			Long id = 1L;
			when(usuarioService.deleteUsuario(id)).thenThrow(new Exception("Database error"));

			given()
					.cookie("refreshToken", validToken)
					.when()
					.delete("/usuario/delete/" + id)
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.and()
					.body(containsString("Error en eliminar el usuario:"));
		}
	}

	@Nested
	@DisplayName("GET /usuario/fotos/{nombreFoto}")
	class GetFotosTests {

		@Test
		@DisplayName("Should return 404 when photo not found")
		public void testGetFotos_NotFound() {
			given().cookie("refreshToken", validToken).when().get("/usuario/fotos/nonexistent.jpg").then()
					.statusCode(HttpStatus.NOT_FOUND.value());
		}

		@Test
		@DisplayName("Should return 200 when photo exists")
		public void testGetFotos_Success() throws Exception {
			String fileName = "test-photo-for-unit-test.jpg";
			Path p = Paths.get(fileName);
			try {
				Files.write(p, "dummy".getBytes());

				given().cookie("refreshToken", validToken).when().get("/usuario/fotos/" + fileName)
						.then()
						.statusCode(HttpStatus.OK.value());
			} finally {
				try {
					Files.deleteIfExists(p);
				} catch (Exception ignored) {
				}
			}
		}

		@Test
		@DisplayName("Should return 500 when probeContentType throws IOException")
		public void testGetFotos_ProbeIOException() throws Exception {
			String fileName = "test-photo-probe-io.jpg";
			Path p = Paths.get(fileName).normalize();
			try {
				Files.write(p, "dummy".getBytes());

				try (MockedStatic<Files> mocked = Mockito.mockStatic(Files.class)) {
					mocked.when(() -> Files.probeContentType(org.mockito.ArgumentMatchers.isA(Path.class)))
							.thenThrow(new IOException("mock IO"));

					Authentication auth = new UsernamePasswordAuthenticationToken("testUser", null,
							List.of(new SimpleGrantedAuthority("ROLE_USER")));
					SecurityContextHolder.getContext().setAuthentication(auth);
					try {
						ResponseEntity<?> resp = usuarioController.getFotos(fileName);
						assertThat(resp.getStatusCode().value())
								.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
						assertThat(resp.getBody().toString())
								.contains("Error determinando el tipo de contenido.");
					} finally {
						SecurityContextHolder.clearContext();
					}
				}
			} finally {
				try {
					Files.deleteIfExists(p);
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Nested
	@DisplayName("POST /usuario/subeFotos")
	class SubeFotosTests {

		@Test
		@DisplayName("Should accept svg upload and return 200")
		public void testUploadImages_Svg() throws Exception {
			try {

				given().cookie("refreshToken", validToken)
						.multiPart("files", "unit-test.svg", "<svg></svg>".getBytes(), "image/svg+xml")
						.when()
						.post("/usuario/subeFotos").then().statusCode(HttpStatus.OK.value());
			} finally {
				try (Stream<Path> paths = Files.walk(Paths.get("."))) {
					paths.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().endsWith("unit-test.svg"))
							.forEach(path -> {
								try {
									Files.delete(path);
									System.out.println("Eliminado: " + path.getFileName());
								} catch (IOException e) {
									System.err.println("No se pudo borrar: " + path);
								}
							});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Test
		@DisplayName("Should convert JPG to WebP and return 200")
		public void testUploadImages_JpgToWebp() throws Exception {
			try {
				// Create a simple JPG image (1x1 pixel)
				BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
				img.setRGB(0, 0, 0xFF0000); // Red pixel
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(img, "jpg", baos);
				byte[] jpgBytes = baos.toByteArray();

				given().cookie("refreshToken", validToken)
						.multiPart("files", "test.jpg", jpgBytes, "image/jpeg")
						.when()
						.post("/usuario/subeFotos")
						.then()
						.statusCode(HttpStatus.OK.value())
						.body("size()", Matchers.greaterThan(0));

			} finally {
				try (Stream<Path> paths = Files.walk(Paths.get("."))) {
					paths.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().matches(".*test.*\\.webp$"))
							.forEach(path -> {
								try {
									Files.delete(path);
									System.out.println("Eliminado: " + path.getFileName());
								} catch (IOException e) {
									System.err.println("No se pudo borrar: " + path);
								}
							});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Test
		@DisplayName("Should accept PNG upload and convert to WebP")
		public void testUploadImages_PngToWebp() throws Exception {
			try {
				// Create a simple PNG image (1x1 pixel)
				BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
				img.setRGB(0, 0, 0x00FF00); // Green pixel
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(img, "png", baos);
				byte[] pngBytes = baos.toByteArray();

				given().cookie("refreshToken", validToken)
						.multiPart("files", "test-image.png", pngBytes, "image/png")
						.when()
						.post("/usuario/subeFotos")
						.then()
						.statusCode(HttpStatus.OK.value())
						.body("size()", Matchers.greaterThan(0));

			} finally {
				try (Stream<Path> paths = Files.walk(Paths.get("."))) {
					paths.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().matches(".*test-image.*\\.webp$"))
							.forEach(path -> {
								try {
									Files.delete(path);
									System.out.println("Eliminado: " + path.getFileName());
								} catch (IOException e) {
									System.err.println("No se pudo borrar: " + path);
								}
							});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Test
		@DisplayName("Should return 500 when IOException occurs during image conversion")
		public void testUploadImages_IOExceptionDuringConversion() throws Exception {
			BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, 0xFF0000);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", baos);
			byte[] jpgBytes = baos.toByteArray();

			org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
					"files",
					"test.jpg",
					"image/jpeg",
					jpgBytes);

			try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
				mockedImageIO.when(() -> ImageIO.read(org.mockito.ArgumentMatchers.any(java.io.InputStream.class)))
						.thenThrow(new IOException("Mock IO error during image read"));

				Authentication auth = new UsernamePasswordAuthenticationToken("testUser", null,
						List.of(new SimpleGrantedAuthority("ROLE_USER")));
				SecurityContextHolder.getContext().setAuthentication(auth);
				try {
					ResponseEntity<?> resp = usuarioController
							.uploadImages(new org.springframework.web.multipart.MultipartFile[] { mockFile });
					assertThat(resp.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
					assertThat(resp.getBody().toString()).contains("Error en convertir la imagen:");
				} finally {
					SecurityContextHolder.clearContext();
				}
			}
		}

		@Test
		@DisplayName("Should return 500 when RuntimeException occurs during image conversion")
		public void testUploadImages_RuntimeExceptionDuringConversion() throws Exception {
			BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, 0x00FF00);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", baos);
			byte[] jpgBytes = baos.toByteArray();

			MockMultipartFile mockFile = new MockMultipartFile(
					"files",
					"test.jpg",
					"image/jpeg",
					jpgBytes);

			try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class, Mockito.CALLS_REAL_METHODS)) {
				mockedImageIO.when(() -> ImageIO.getImageWritersByMIMEType("image/webp"))
						.thenThrow(new RuntimeException("Mock error: No WebP writer available"));

				Authentication auth = new UsernamePasswordAuthenticationToken("testUser", null,
						List.of(new SimpleGrantedAuthority("ROLE_USER")));
				SecurityContextHolder.getContext().setAuthentication(auth);
				try {
					ResponseEntity<?> resp = usuarioController
							.uploadImages(new org.springframework.web.multipart.MultipartFile[] { mockFile });
					assertThat(resp.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
				} finally {
					SecurityContextHolder.clearContext();
				}
			}
		}

		@Test
		@DisplayName("Should return 500 when IOException occurs during SVG copy")
		public void testUploadImages_IOExceptionDuringSVGCopy() throws Exception {
			MockMultipartFile mockFile = new MockMultipartFile(
					"files",
					"test.svg",
					"image/svg+xml",
					"<svg></svg>".getBytes());

			try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
				mockedFiles.when(() -> Files.copy(ArgumentMatchers.any(java.io.InputStream.class),
						ArgumentMatchers.any(Path.class),
						ArgumentMatchers.any(CopyOption[].class)))
						.thenThrow(new IOException("Mock error: Cannot write SVG file"));

				Authentication auth = new UsernamePasswordAuthenticationToken("testUser", null,
						List.of(new SimpleGrantedAuthority("ROLE_USER")));
				SecurityContextHolder.getContext().setAuthentication(auth);
				try {
					ResponseEntity<?> resp = usuarioController
							.uploadImages(new MultipartFile[] { mockFile });
					assertThat(resp.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
					assertThat(resp.getBody().toString()).contains("Error en guardar la imagen:");
				} finally {
					SecurityContextHolder.clearContext();
				}
			}
		}
	}

	@LocalServerPort
	private int port;

	@Autowired
	private UsuarioController usuarioController;

	@MockitoBean
	private UsuarioService usuarioService;

	@Autowired
	private JwtUtil jwtUtil;

	private String validToken;

	private String adminToken;

	@BeforeEach
	public void setUp() {
		RestAssured.baseURI = "https://localhost";
		RestAssured.port = port;
		RestAssured.basePath = "";
		RestAssured.useRelaxedHTTPSValidation();

		Authentication auth = new UsernamePasswordAuthenticationToken("testUser", null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		validToken = jwtUtil.generateToken(auth, "access", 1L);

		Authentication adminAuth = new UsernamePasswordAuthenticationToken("adminUser", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		adminToken = jwtUtil.generateToken(adminAuth, "access", 99L);
	}
}
