package com.sovereingschool.back_common.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sovereingschool.back_common.DTOs.ClaseDTO;
import com.sovereingschool.back_common.DTOs.CursoDTO;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.PlanRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

/**
 * Tests unitarios para la clase {@link CursoUtil}.
 */
@ExtendWith(MockitoExtension.class)
class CursoUtilTest {

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private PlanRepository planRepo;

    private CursoUtil cursoUtil;

    @BeforeEach
    void setUp() {
        cursoUtil = new CursoUtil(usuarioRepo, planRepo);
    }

    /**
     * Prueba la conversión de un {@link ClaseDTO} a un modelo {@link Clase}.
     */
    @Test
    void convertirClaseDTOToClase_DeberiaMapearCamposCorrectamente() {
        ClaseDTO dto = new ClaseDTO(1L, "Clase 1", "Desc", "Cont", 0, "path", 1, 100L);
        Clase clase = cursoUtil.claseDTOToClase(dto);

        assertNotNull(clase, "La clase resultante no debería ser nula");
        assertEquals(1L, clase.getIdClase(), "El ID de la clase debe coincidir");
        assertEquals("Clase 1", clase.getNombreClase(), "El nombre de la clase debe coincidir");
        assertEquals("Desc", clase.getDescripcionClase(), "La descripción debe coincidir");
        assertEquals("Cont", clase.getContenidoClase(), "El contenido debe coincidir");
        assertEquals(0, clase.getTipoClase(), "El tipo de clase debe coincidir");
        assertEquals("path", clase.getDireccionClase(), "La dirección debe coincidir");
        assertEquals(1, clase.getPosicionClase(), "La posición debe coincidir");
    }

    /**
     * Prueba la conversión exitosa de un {@link CursoDTO} a un modelo
     * {@link Curso}.
     */
    @Test
    void convertirCursoDTOToCurso_Exitoso() throws NotFoundException {
        Long profId = 1L;
        Long planId = 1L;
        Usuario prof = new Usuario();
        prof.setIdUsuario(profId);
        Plan plan = new Plan();
        plan.setIdPlan(planId);

        when(usuarioRepo.findById(profId)).thenReturn(Optional.of(prof));
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));

        ClaseDTO claseDTO = new ClaseDTO(1L, "Clase 1", "Desc", "Cont", 0, "path", 1, 100L);
        CursoDTO cursoDTO = new CursoDTO(
                1L, "Curso 1", List.of(profId), new Date(), List.of(claseDTO), List.of(planId),
                "Desc corta", "Desc larga", "img.png", BigDecimal.valueOf(10.0));

        Curso curso = cursoUtil.cursoDTOToCurso(cursoDTO);

        assertNotNull(curso, "El curso resultante no debería ser nulo");
        assertEquals(1L, curso.getIdCurso(), "El ID del curso debe coincidir");
        assertEquals("Curso 1", curso.getNombreCurso(), "El nombre del curso debe coincidir");
        assertEquals(1, curso.getProfesoresCurso().size(), "Debería haber un profesor");
        assertEquals(1, curso.getClasesCurso().size(), "Debería haber una clase");
        assertEquals(1, curso.getPlanesCurso().size(), "Debería haber un plan");
    }

    /**
     * Prueba que se lanza {@link NotFoundException} cuando el profesor no existe.
     */
    @Test
    void convertirCursoDTOToCurso_ProfesorNoEncontrado_DeberiaLanzarExcepcion() {
        CursoDTO cursoDTO = new CursoDTO(
                1L, "Curso 1", List.of(99L), new Date(), List.of(), List.of(),
                "Desc corta", "Desc larga", "img.png", BigDecimal.valueOf(10.0));

        when(usuarioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cursoUtil.cursoDTOToCurso(cursoDTO),
                "Debería lanzar NotFoundException");
    }

    @Test
    void testCursoToCursoDTO() {
        Usuario prof = new Usuario();
        prof.setIdUsuario(1L);

        Clase clase = new Clase();
        clase.setIdClase(1L);
        clase.setNombreClase("Clase 1");

        Curso curso = new Curso();
        curso.setIdCurso(1L);
        curso.setNombreCurso("Curso 1");
        curso.setProfesoresCurso(List.of(prof));
        curso.setClasesCurso(List.of(clase));
        curso.setPlanesCurso(List.of());

        CursoDTO dto = cursoUtil.cursoToCursoDTO(curso);

        assertNotNull(dto);
        assertEquals(1L, dto.idCurso());
        assertEquals(1, dto.profesoresCurso().size());
        assertEquals(1, dto.clasesCurso().size());
    }

    @Test
    void testCursoDTOToCurso_NullLists() throws NotFoundException {
        CursoDTO cursoDTO = new CursoDTO(
                1L, "Curso 1", List.of(), new Date(), List.of(), List.of(),
                "Desc corta", "Desc larga", "img.png", BigDecimal.valueOf(10.0));

        Curso curso = cursoUtil.cursoDTOToCurso(cursoDTO);

        assertNotNull(curso);
        assertEquals(0, curso.getProfesoresCurso().size());
        assertEquals(0, curso.getClasesCurso().size());
        assertEquals(0, curso.getPlanesCurso().size());
    }

    @Test
    void testClaseToClaseDTO_NullCurso() {
        Clase clase = new Clase();
        clase.setIdClase(1L);
        clase.setNombreClase("Clase 1");
        clase.setCursoClase(null);

        ClaseDTO dto = cursoUtil.claseToClaseDTO(clase);

        assertNotNull(dto);
        assertNull(dto.cursoClase());
    }
}
