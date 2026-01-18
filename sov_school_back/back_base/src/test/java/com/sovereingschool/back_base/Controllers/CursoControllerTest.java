package com.sovereingschool.back_base.Controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
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

import com.sovereingschool.back_base.Interfaces.ICursoService;
import com.sovereingschool.back_common.DTOs.ClaseDTO;
import com.sovereingschool.back_common.DTOs.CursoDTO;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Utils.CursoUtil;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityNotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CursoControllerTest {

    @Nested
    class GetAllTests {

        @Test
        public void testGetAll_Success() {
            // GIVEN
            Curso curso = new Curso();
            curso.setIdCurso(1L);
            curso.setNombreCurso("Java Spring");
            CursoDTO dto = new CursoDTO(1L, "Java Spring", null, null, null, null, null, null, null, null);

            when(cursoService.getAll()).thenReturn(Collections.singletonList(curso));
            when(cursoUtil.cursoToCursoDTO(curso)).thenReturn(dto);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getAll")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("$", hasSize(1))
                    .body("[0].idCurso", is(1))
                    .body("[0].nombreCurso", equalTo("Java Spring"));
        }

        @Test
        public void testGetAll_InternalError() {
            when(cursoService.getAll()).thenThrow(new RuntimeException("Error de BD"));

            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getAll")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener los cursos: Error de BD"));
        }
    }

    @Nested
    class GetCursoByIdTests {

        @Test
        public void testGetCurso_Success() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            Curso curso = new Curso();
            curso.setIdCurso(id);
            curso.setNombreCurso("Curso Expert");
            CursoDTO dto = new CursoDTO(id, "Curso Expert", null, null, null, null, null, null, null, null);

            when(cursoService.getCurso(id)).thenReturn(curso);
            when(cursoUtil.cursoToCursoDTO(curso)).thenReturn(dto);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("idCurso", is(id.intValue()))
                    .body("nombreCurso", equalTo("Curso Expert"));
        }

        @Test
        public void testGetCurso_NotFound() throws NotFoundException {
            // GIVEN
            Long id = 99L;
            when(cursoService.getCurso(id)).thenThrow(new EntityNotFoundException("No existe"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("No existe"));
        }

        @Test
        public void testGetCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getCurso(id)).thenThrow(new RuntimeException("Fallo crítico"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener el curso: Fallo crítico"));
        }
    }

    @Nested
    class GetNombreCursoTests {

        @Test
        public void testGetNombreCurso_Success() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            String nombreEsperado = "Programación Funcional";

            // Mockeamos solo el servicio (aquí no se usa CursoUtil según tu código)
            when(cursoService.getNombreCurso(id)).thenReturn(nombreEsperado);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombreCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    // Como la respuesta es un String plano, comparamos el body directamente
                    .body(equalTo(nombreEsperado));
        }

        @Test
        public void testGetNombreCurso_NotFound() throws NotFoundException {
            // GIVEN
            Long id = 99L;
            String errorMsg = "Curso no encontrado";
            when(cursoService.getNombreCurso(id)).thenThrow(new EntityNotFoundException(errorMsg));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombreCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo(errorMsg));
        }

        @Test
        public void testGetNombreCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getNombreCurso(id)).thenThrow(new RuntimeException("Error interno"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombreCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener el nombre del curso: Error interno"));
        }
    }

    @Nested
    class GetNombresProfesoresCursoTests {

        @Test
        public void testGetNombresProfesoresCurso_Success() throws NotFoundException {
            // GIVEN
            Long idCurso = 1L;

            Usuario prof1 = new Usuario();
            prof1.setNombreUsuario("Dr. Strange");

            Usuario prof2 = new Usuario();
            prof2.setNombreUsuario("Prof. X");

            List<Usuario> listaProfesores = Arrays.asList(prof1, prof2);

            when(cursoService.getProfesoresCurso(idCurso)).thenReturn(listaProfesores);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombresProfesoresCurso/{id}", idCurso)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("$", hasSize(2)) // Comprueba el tamaño de la lista
                    .body("$", hasItems("Dr. Strange", "Prof. X")) // Comprueba que contiene estos nombres
                    .body("[0]", equalTo("Dr. Strange")); // Comprueba una posición específica
        }

        @Test
        public void testGetNombresProfesoresCurso_Empty() throws NotFoundException {
            // GIVEN: El curso existe pero no tiene profesores asignados
            Long idCurso = 1L;
            when(cursoService.getProfesoresCurso(idCurso)).thenReturn(Collections.emptyList());

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombresProfesoresCurso/{id}", idCurso)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("$", hasSize(0));
        }

        @Test
        public void testGetNombresProfesoresCurso_NotFound() throws NotFoundException {
            // GIVEN
            Long idInexistente = 99L;
            when(cursoService.getProfesoresCurso(idInexistente))
                    .thenThrow(new EntityNotFoundException("Curso no encontrado"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombresProfesoresCurso/{id}", idInexistente)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no encontrado"));
        }

        @Test
        public void testGetNombresProfesoresCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getProfesoresCurso(id)).thenThrow(new RuntimeException("Error de conexión"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getNombresProfesoresCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener los nombres de los profesores: Error de conexión"));
        }
    }

    @Nested
    class GetFechaCreacionCursoTests {

        @Test
        public void testGetFechaCreacionCurso_Success() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            java.util.Date fechaSimulada = new java.util.Date();

            when(cursoService.getFechaCreacionCurso(id)).thenReturn(fechaSimulada);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getFechaCreacionCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    // Verificamos que el cuerpo no sea nulo.
                    // Si sabes el formato exacto (ej. ISO), puedes usar equalTo.
                    .body(notNullValue());
        }

        @Test
        public void testGetFechaCreacionCurso_NotFound() throws NotFoundException {
            // GIVEN
            Long id = 99L;
            when(cursoService.getFechaCreacionCurso(id))
                    .thenThrow(new EntityNotFoundException("Fecha no encontrada"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getFechaCreacionCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Fecha no encontrada"));
        }

        @Test
        public void testGetFechaCreacionCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getFechaCreacionCurso(id))
                    .thenThrow(new RuntimeException("Fallo de sistema"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getFechaCreacionCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener la fecha del curso: Fallo de sistema"));
        }
    }

    @Nested
    class GetClasesDelCursoTests {

        @Test
        public void testGetClasesDelCurso_Success() throws NotFoundException {
            // GIVEN
            Long idCurso = 1L;
            Clase clase1 = new Clase(); // Asume que tienes este modelo
            clase1.setIdClase(101L);

            ClaseDTO dto1 = new ClaseDTO(101L, "Introducción a Spring", null, null, null, null, null, null);

            when(cursoService.getClasesDelCurso(idCurso)).thenReturn(Collections.singletonList(clase1));
            when(cursoUtil.claseToClaseDTO(clase1)).thenReturn(dto1);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getClasesDelCurso/{id}", idCurso)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("$", hasSize(1))
                    .body("[0].idClase", is(101))
                    .body("[0].nombreClase", equalTo("Introducción a Spring"));
        }

        @Test
        public void testGetClasesDelCurso_Empty_ReturnsNotFound() throws NotFoundException {
            // GIVEN: El servicio devuelve lista vacía, lo que activa tu
            // if(clases.isEmpty())
            Long idCurso = 1L;
            when(cursoService.getClasesDelCurso(idCurso)).thenReturn(Collections.emptyList());

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getClasesDelCurso/{id}", idCurso)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no encontrado"));
        }

        @Test
        public void testGetClasesDelCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getClasesDelCurso(id)).thenThrow(new RuntimeException("Error fatal"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getClasesDelCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener las clases del curso: Error fatal"));
        }
    }

    @Nested
    class GetPlanesDelCursoTests {

        @Test
        public void testGetPlanesDelCurso_Success() throws NotFoundException {
            // GIVEN
            Long idCurso = 1L;

            // Suponiendo que Plan tiene un constructor o setters
            Plan plan1 = new Plan();
            plan1.setIdPlan(500L);
            plan1.setNombrePlan("Plan Premium");

            List<Plan> listaPlanes = Arrays.asList(plan1);

            when(cursoService.getPlanesDelCurso(idCurso)).thenReturn(listaPlanes);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getPlanesDelCurso/{id}", idCurso)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("$", hasSize(1))
                    .body("[0].idPlan", is(500))
                    .body("[0].nombrePlan", equalTo("Plan Premium"));
        }

        @Test
        public void testGetPlanesDelCurso_Empty_ReturnsNotFound() throws NotFoundException {
            // GIVEN
            Long idCurso = 1L;
            when(cursoService.getPlanesDelCurso(idCurso)).thenReturn(Collections.emptyList());

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getPlanesDelCurso/{id}", idCurso)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no encontrado"));
        }

        @Test
        public void testGetPlanesDelCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getPlanesDelCurso(id)).thenThrow(new RuntimeException("Error en base de datos"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getPlanesDelCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener los planes del curso: Error en base de datos"));
        }
    }

    @Nested
    class GetPrecioCursoTests {

        @Test
        public void testGetPrecioCurso_Success() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            java.math.BigDecimal precioSimulado = new java.math.BigDecimal("99.99");
            when(cursoService.getPrecioCurso(id)).thenReturn(precioSimulado);

            // WHEN
            float precioRespuesta = given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getPrecioCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(Float.class); // Extraemos el cuerpo directamente como Float

            // THEN (Assert de JUnit puro, esto no falla nunca)
            org.junit.jupiter.api.Assertions.assertEquals(99.99f, precioRespuesta, 0.001);
        }

        @Test
        public void testGetPrecioCurso_NotFound() throws NotFoundException {
            // GIVEN
            Long id = 99L;
            when(cursoService.getPrecioCurso(id))
                    .thenThrow(new EntityNotFoundException("Precio no disponible"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getPrecioCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Precio no disponible"));
        }

        @Test
        public void testGetPrecioCurso_InternalError() throws NotFoundException {
            // GIVEN
            Long id = 1L;
            when(cursoService.getPrecioCurso(id))
                    .thenThrow(new RuntimeException("Error de cálculo"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + JWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/getPrecioCurso/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en obtener el precio del curso: Error de cálculo"));
        }
    }

    @Nested
    class UpdateCursoTests {

        private String adminJWT;

        @BeforeEach
        public void setUp() {
            // 1. Creamos una autoridad con el rol que pide el @PreAuthorize
            // Nota: Spring Security suele esperar "ROLE_ADMIN" si en el PreAuthorize
            // pusiste 'ADMIN'
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");

            // 2. Creamos el objeto Authentication simulado
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "adminUser",
                    null,
                    List.of(authority));

            // 3. Generamos el token usando tu utilidad (asumiendo tipo "access" e id 1L)
            adminJWT = jwtUtil.generateToken(auth, "access", 1L);
        }

        @Test
        public void testUpdateCurso_Success() throws Exception {
            // GIVEN
            CursoDTO inputDto = new CursoDTO(1L, "Java Updated", null, null, null, null, null, null, null, null);
            Curso cursoMock = new Curso();
            cursoMock.setIdCurso(1L);
            cursoMock.setNombreCurso("Java Updated");
            CursoDTO outputDto = new CursoDTO(1L, "Java Updated", null, null, null, null, null, null, null, null);

            when(cursoUtil.cursoDTOToCurso(any(CursoDTO.class))).thenReturn(cursoMock);
            when(cursoService.updateCurso(any(Curso.class))).thenReturn(cursoMock);
            when(cursoUtil.cursoToCursoDTO(any(Curso.class))).thenReturn(outputDto);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT) // Usamos el token con privilegios
                    .contentType(ContentType.JSON)
                    .body(inputDto)
                    .when()
                    .put("/update")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("idCurso", is(1))
                    .body("nombreCurso", equalTo("Java Updated"));
        }

        @Test
        public void testUpdateCurso_NotFound() throws Exception {
            // GIVEN
            CursoDTO inputDto = new CursoDTO(99L, "Inexistente", null, null, null, null, null, null, null, null);

            when(cursoUtil.cursoDTOToCurso(any(CursoDTO.class))).thenReturn(new Curso());
            when(cursoService.updateCurso(any(Curso.class)))
                    .thenThrow(new NotFoundException("Curso no encontrado"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .body(inputDto)
                    .when()
                    .put("/update")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no encontrado"));
        }

        @Test
        public void testUpdateCurso_FailedDependency() throws Exception {
            // GIVEN
            CursoDTO inputDto = new CursoDTO(1L, "Test", null, null, null, null, null, null, null, null);

            when(cursoUtil.cursoDTOToCurso(any(CursoDTO.class))).thenReturn(new Curso());
            when(cursoService.updateCurso(any(Curso.class)))
                    .thenThrow(new com.sovereingschool.back_common.Exceptions.InternalComunicationException(
                            "Error comunicación"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .body(inputDto)
                    .when()
                    .put("/update")
                    .then()
                    .statusCode(HttpStatus.FAILED_DEPENDENCY.value())
                    .body(equalTo("Error comunicación"));
        }

        @Test
        public void testUpdateCurso_InternalServerError() throws Exception {
            // GIVEN
            CursoDTO inputDto = new CursoDTO(1L, "Error Test", null, null, null, null, null, null, null, null);

            when(cursoUtil.cursoDTOToCurso(any(CursoDTO.class))).thenReturn(new Curso());
            when(cursoService.updateCurso(any(Curso.class)))
                    .thenThrow(new com.sovereingschool.back_common.Exceptions.InternalServerException(
                            "Error interno de servidor"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .body(inputDto)
                    .when()
                    .put("/update")
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(equalTo("Error interno de servidor"));
        }

        @Test
        public void testUpdateCurso_NotModified() throws Exception {
            // GIVEN
            CursoDTO inputDto = new CursoDTO(1L, "Sin Cambios", null, null, null, null, null, null, null, null);

            when(cursoUtil.cursoDTOToCurso(any(CursoDTO.class))).thenReturn(new Curso());
            when(cursoService.updateCurso(any(Curso.class)))
                    .thenThrow(new com.sovereingschool.back_common.Exceptions.RepositoryException(
                            "No se pudo modificar en base de datos"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .body(inputDto)
                    .when()
                    .put("/update")
                    .then()
                    .statusCode(HttpStatus.NOT_MODIFIED.value());
            // Nota: HTTP 304 (Not Modified) a menudo no devuelve cuerpo,
            // dependiendo de la configuración de Spring.
        }
    }

    @Nested
    class DeleteCursoTests {

        private String adminJWT;

        @BeforeEach
        public void setUp() {
            // Reutilizamos la lógica del token con rol ADMIN
            org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "adminUser",
                    null,
                    List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
            adminJWT = jwtUtil.generateToken(auth, "access", 1L);
        }

        @Test
        public void testDeleteCurso_Success() throws Exception {
            // GIVEN
            Long id = 1L;

            // Si devuelve Boolean, usa: .thenReturn(true)
            // Si devuelve Object, usa: .thenReturn(new Object())
            // Usamos any() para mayor flexibilidad
            when(cursoService.deleteCurso(id)).thenReturn(true);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .delete("/delete/{id}", id)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo("Curso eliminado con éxito!!!"));
        }

        @Test
        public void testDeleteCurso_NotFound() throws Exception {
            // GIVEN
            Long id = 99L;
            org.mockito.Mockito.doThrow(new EntityNotFoundException("Curso no existe"))
                    .when(cursoService).deleteCurso(id);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .delete("/delete/{id}", id)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no existe"));
        }

        @Test
        public void testDeleteCurso_InternalError() throws Exception {
            // GIVEN
            Long id = 1L;
            org.mockito.Mockito.doThrow(new RuntimeException("Fallo en la base de datos"))
                    .when(cursoService).deleteCurso(id);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .delete("/delete/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(equalTo("Fallo en la base de datos"));
        }

        @Test
        public void testDeleteCurso_GeneralException() throws Exception {
            // GIVEN
            Long id = 1L;
            String mensajeError = "Error inesperado";

            // Usamos una excepción que Mockito permita pero que el catch de
            // RuntimeException no capture
            // Si el servicio no tiene 'throws Exception', lanzamos un error de
            // inicialización o similar
            when(cursoService.deleteCurso(id))
                    .thenAnswer(invocation -> {
                        throw new Exception(mensajeError);
                    });

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .delete("/delete/{id}", id)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(equalTo("Error en eliminar el curso: " + mensajeError));
        }
    }

    @Nested
    class GetClaseForIdTests {

        private String userJWT;

        @BeforeEach
        public void setUp() {
            // Generamos un token con ROLE_USER
            org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "standardUser",
                    null,
                    java.util.List
                            .of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
            userJWT = jwtUtil.generateToken(auth, "access", 123L);
        }

        @Test
        public void testGetClaseForId_Success() throws Exception {
            // GIVEN
            Long idCurso = 1L;
            Long idClase = 50L;

            Clase claseMock = new Clase();
            claseMock.setIdClase(idClase);
            claseMock.setNombreClase("Lección de Java");

            Curso cursoMock = new Curso();
            cursoMock.setIdCurso(idCurso);
            cursoMock.setClasesCurso(java.util.List.of(claseMock));

            ClaseDTO dto = new ClaseDTO(idClase, "Lección de Java", null, null, null, null, null, null);

            when(cursoService.getCurso(idCurso)).thenReturn(cursoMock);
            when(cursoUtil.claseToClaseDTO(claseMock)).thenReturn(dto);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + userJWT)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/{idCurso}/getClaseForId/{idClase}", idCurso, idClase)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("idClase", is(idClase.intValue()))
                    .body("nombreClase", equalTo("Lección de Java"));
        }

        @Test
        public void testGetClaseForId_ClassNotFoundInCurso() throws Exception {
            // GIVEN: El curso existe pero no tiene esa clase
            Long idCurso = 1L;
            Long idClaseInexistente = 999L;

            Curso cursoMock = new Curso();
            cursoMock.setIdCurso(idCurso);
            cursoMock.setClasesCurso(java.util.Collections.emptyList());

            when(cursoService.getCurso(idCurso)).thenReturn(cursoMock);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + userJWT)
                    .when()
                    .get("/{idCurso}/getClaseForId/{idClase}", idCurso, idClaseInexistente)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Clase no encontrada"));
        }

        @Test
        public void testGetClaseForId_CursoNotFound() throws Exception {
            // GIVEN: El curso ni siquiera existe
            Long idCurso = 404L;
            when(cursoService.getCurso(idCurso)).thenThrow(new EntityNotFoundException("Curso no existe"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + userJWT)
                    .when()
                    .get("/{idCurso}/getClaseForId/{idClase}", idCurso, 1L)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no existe"));
        }

        @Test
        public void testGetClaseForId_InternalError() throws Exception {
            // GIVEN
            when(cursoService.getCurso(anyLong())).thenThrow(new RuntimeException("Error imprevisto"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + userJWT)
                    .when()
                    .get("/{idCurso}/getClaseForId/{idClase}", 1L, 1L)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error al encontrar la clase: Error imprevisto"));
        }
    }

    @Nested
    class DeleteClaseTests {

        private String adminJWT;

        @BeforeEach
        public void setUp() {
            // Token con rol ADMIN para permitir el borrado
            org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            adminJWT = jwtUtil.generateToken(auth, "access", 1L);
        }

        @Test
        public void testDeleteClase_Success() throws Exception {
            // GIVEN
            Long idCurso = 1L;
            Long idClaseAEliminar = 10L;

            // Simulamos dos clases: la 10 (a borrar) y la 11 (que bajará de posición)
            Clase clase1 = new Clase();
            clase1.setIdClase(idClaseAEliminar);
            clase1.setPosicionClase(1);

            Clase clase2 = new Clase();
            clase2.setIdClase(11L);
            clase2.setPosicionClase(2);

            // Lista mutable (importante usar ArrayList porque el controlador hará .remove)
            List<Clase> clases = new java.util.ArrayList<>(List.of(clase1, clase2));

            Curso cursoMock = new Curso();
            cursoMock.setIdCurso(idCurso);
            cursoMock.setClasesCurso(clases);

            when(cursoService.getCurso(idCurso)).thenReturn(cursoMock);
            // No necesitamos mockear el retorno de update o delete si son void/objetos
            // simples
            when(cursoService.updateCurso(any(Curso.class))).thenReturn(cursoMock);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .when()
                    .delete("/{idCurso}/deleteClase/{idClase}", idCurso, idClaseAEliminar)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo("Clase eliminada con exito!!!"));

            // Verificamos que se llamó al borrado de la clase específica
            verify(cursoService, times(1)).deleteClase(clase1);
        }

        @Test
        public void testDeleteClase_NotFound() throws Exception {
            // GIVEN: El curso existe pero la clase no está en su lista
            Long idCurso = 1L;
            Long idClaseInexistente = 999L;

            Curso cursoMock = new Curso();
            cursoMock.setIdCurso(idCurso);
            cursoMock.setClasesCurso(new java.util.ArrayList<>()); // Lista vacía

            when(cursoService.getCurso(idCurso)).thenReturn(cursoMock);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .when()
                    .delete("/{idCurso}/deleteClase/{idClase}", idCurso, idClaseInexistente)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Clase no encontrada"));
        }

        @Test
        public void testDeleteClase_CursoNotFound() throws Exception {
            // GIVEN: El curso no existe
            Long idCurso = 1L;
            when(cursoService.getCurso(idCurso)).thenThrow(new EntityNotFoundException("Curso inexistente"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .when()
                    .delete("/{idCurso}/deleteClase/{idClase}", idCurso, 10L)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso inexistente"));
        }

        @Test
        public void testDeleteClase_InternalError() throws Exception {
            // GIVEN: Error inesperado al borrar
            when(cursoService.getCurso(anyLong())).thenThrow(new RuntimeException("Error de DB"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .when()
                    .delete("/{idCurso}/deleteClase/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Error en borrar la clase: Error de DB"));
        }
    }

    @Nested
    class SubeVideoTests {

        private String adminJWT;

        @BeforeEach
        public void setUp() {
            // Token con privilegios para PROF/ADMIN
            org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "admin", null, java.util.List
                            .of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
            adminJWT = jwtUtil.generateToken(auth, "access", 1L);
        }

        @Test
        public void testSubeVideo_Success() throws Exception {
            // GIVEN
            Long idCurso = 1L;
            Long idClase = 10L;
            String pathSimulado = "https://s3.bucket.com/video123.mp4";

            // Creamos un archivo simulado (byte array, nombre del parámetro, nombre del
            // archivo)
            byte[] content = "video content".getBytes();

            when(cursoService.subeVideo(any(org.springframework.web.multipart.MultipartFile.class)))
                    .thenReturn(pathSimulado);

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    // RestAssured detecta automáticamente el Content-Type multipart/form-data
                    .multiPart("video", "clase1.mp4", content)
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", idCurso, idClase)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(equalTo(pathSimulado));
        }

        @Test
        public void testSubeVideo_FileEmpty() {
            // GIVEN
            byte[] emptyContent = new byte[0];

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "vacio.mp4", emptyContent)
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body(equalTo("Archivo vacío"));
        }

        @Test
        public void testSubeVideo_IllegalArgument() throws Exception {
            // GIVEN
            when(cursoService.subeVideo(any())).thenThrow(new IllegalArgumentException("Formato no válido"));

            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "test.txt", "contenido".getBytes())
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body(equalTo("Formato no válido"));
        }

        @Test
        public void testSubeVideo_GeneralException() throws Exception {
            // GIVEN
            // Usamos thenAnswer para "saltar" la validación de checked exceptions si fuera
            // necesario
            when(cursoService.subeVideo(any())).thenAnswer(inv -> {
                throw new Exception("Fallo de IO");
            });

            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "video.mp4", "content".getBytes())
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(equalTo("Error en subir el video: Fallo de IO"));
        }

        @Test
        public void testSubeVideo_AccessDenied() throws Exception {
            // GIVEN: El servicio lanza una AccessDeniedException
            // (org.springframework.security.access.AccessDeniedException)
            when(cursoService.subeVideo(any()))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException(
                            "No tienes permiso para subir archivos"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "video.mp4", "content".getBytes())
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .body(equalTo("No tienes permiso para subir archivos"));
        }

        @Test
        public void testSubeVideo_IllegalState() throws Exception {
            // GIVEN: El servicio lanza IllegalStateException (ej: disco lleno o buffer
            // corrupto)
            when(cursoService.subeVideo(any()))
                    .thenThrow(new IllegalStateException("Estado de servidor no válido"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "video.mp4", "content".getBytes())
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body(equalTo("Estado de servidor no válido"));
        }

        @Test
        public void testSubeVideo_EntityNotFound() throws Exception {
            // GIVEN: No se encuentra el curso o clase al que asociar el video
            when(cursoService.subeVideo(any()))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Curso no encontrado para el video"));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "video.mp4", "content".getBytes())
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body(equalTo("Curso no encontrado para el video"));
        }

        @Test
        public void testSubeVideo_RuntimeException() throws Exception {
            // GIVEN: Lanzamos una RuntimeException pura
            String errorMsg = "Error inesperado en tiempo de ejecución";
            when(cursoService.subeVideo(any()))
                    .thenThrow(new RuntimeException(errorMsg));

            // WHEN & THEN
            given()
                    .header("Authorization", "Bearer " + adminJWT)
                    .multiPart("video", "video.mp4", "content".getBytes())
                    .when()
                    .post("/subeVideo/{idCurso}/{idClase}", 1L, 10L)
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    // Verificamos que devuelve el mensaje directo, sin el prefijo "Error en subir
                    // el video: "
                    .body(equalTo(errorMsg));
        }
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private ICursoService cursoService;

    @MockitoBean
    private CursoUtil cursoUtil;

    @Autowired
    private JwtUtil jwtUtil;

    private String JWT;

    @BeforeEach
    public void setUp() {
        // Configuración para TLS/SSL y Puerto
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.basePath = "/cursos";
        RestAssured.useRelaxedHTTPSValidation();

        // Generación de Token para seguridad
        JWT = jwtUtil.generateInitToken();
    }
}