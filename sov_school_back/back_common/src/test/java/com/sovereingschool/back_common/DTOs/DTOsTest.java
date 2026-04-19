package com.sovereingschool.back_common.DTOs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;

/**
 * Tests unitarios para validar los DTOs del sistema.
 */
class DTOsTest {

    /**
     * Prueba la creación y validación de campos del {@link ClaseDTO}.
     */
    @Test
    void testClaseDTO_DeberiaMapearCamposCorrectamente() {
        ClaseDTO dto = new ClaseDTO(1L, "Clase", "Desc", "Cont", 0, "Dir", 1, 100L);
        assertEquals(1L, dto.idClase(), "El ID de clase debe coincidir");
        assertEquals("Clase", dto.nombreClase(), "El nombre de clase debe coincidir");
        assertEquals("Desc", dto.descripcionClase(), "La descripción debe coincidir");
        assertEquals("Cont", dto.contenidoClase(), "El contenido debe coincidir");
        assertEquals(0, dto.tipoClase(), "El tipo debe coincidir");
        assertEquals("Dir", dto.direccionClase(), "La dirección debe coincidir");
        assertEquals(1, dto.posicionClase(), "La posición debe coincidir");
        assertEquals(100L, dto.cursoClase(), "El curso asociado debe coincidir");
    }

    /**
     * Prueba la creación y validación de campos del {@link CursoDTO}.
     */
    @Test
    void testCursoDTO_DeberiaMapearCamposCorrectamente() {
        Date now = new Date();
        ClaseDTO claseDto = new ClaseDTO(1L, "Clase", "Desc", "Cont", 0, "Dir", 1, 100L);
        CursoDTO dto = new CursoDTO(100L, "Curso", List.of(1L), now, List.of(claseDto), List.of(1L), "Corta", "Larga",
                "img.png", new BigDecimal("50.00"));

        assertEquals(100L, dto.idCurso(), "El ID de curso debe coincidir");
        assertEquals("Curso", dto.nombreCurso(), "El nombre de curso debe coincidir");
        assertEquals(List.of(1L), dto.profesoresCurso(), "La lista de profesores debe coincidir");
        assertEquals(now, dto.fechaPublicacionCurso(), "La fecha debe coincidir");
        assertEquals(List.of(claseDto), dto.clasesCurso(), "La lista de clases debe coincidir");
        assertEquals(List.of(1L), dto.planesCurso(), "La lista de planes debe coincidir");
        assertEquals("Corta", dto.descripcionCorta(), "La descripción corta debe coincidir");
        assertEquals("Larga", dto.descripcionLarga(), "La descripción larga debe coincidir");
        assertEquals("img.png", dto.imagenCurso(), "La imagen debe coincidir");
        assertEquals(new BigDecimal("50.00"), dto.precioCurso(), "El precio debe coincidir");
    }

    /**
     * Prueba la creación y validación de campos del {@link NewUsuario}.
     */
    @Test
    void testNewUsuario_DeberiaMapearCamposCorrectamente() {
        Date now = new Date();
        Plan plan = new Plan();
        Curso curso = new Curso();
        NewUsuario dto = new NewUsuario("User", "test@test.com", "pass", List.of("foto.png"), plan, List.of(curso),
                now);

        assertEquals("User", dto.nombreUsuario(), "El nombre de usuario debe coincidir");
        assertEquals("test@test.com", dto.correoElectronico(), "El correo debe coincidir");
        assertEquals("pass", dto.password(), "La contraseña debe coincidir");
        assertEquals(List.of("foto.png"), dto.fotoUsuario(), "La lista de fotos debe coincidir");
        assertEquals(plan, dto.planUsuario(), "El plan debe coincidir");
        assertEquals(List.of(curso), dto.cursosUsuario(), "La lista de cursos debe coincidir");
        assertEquals(now, dto.fechaRegistroUsuario(), "La fecha debe coincidir");
    }
}
