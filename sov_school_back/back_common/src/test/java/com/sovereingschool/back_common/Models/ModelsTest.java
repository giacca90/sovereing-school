package com.sovereingschool.back_common.Models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para validar la integridad de los modelos de datos.
 */
class ModelsTest {

    /**
     * Prueba la creación y validación de campos del modelo {@link Usuario}.
     */
    @Test
    void testUsuario_DeberiaMapearCamposCorrectamente() {
        Plan plan = new Plan();
        plan.setIdPlan(1L);
        plan.setNombrePlan("Pro");

        List<Curso> cursos = new ArrayList<>();
        Date now = new Date();

        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombreUsuario("Test User")
                .fotoUsuario(List.of("foto.png"))
                .presentacion("Hello")
                .rollUsuario(RoleEnum.USER)
                .planUsuario(plan)
                .cursosUsuario(cursos)
                .fechaRegistroUsuario(now)
                .isEnabled(true)
                .accountNoExpired(true)
                .accountNoLocked(true)
                .credentialsNoExpired(true)
                .build();

        assertEquals(1L, usuario.getIdUsuario(), "El ID de usuario debe coincidir");
        assertEquals("Test User", usuario.getNombreUsuario(), "El nombre de usuario debe coincidir");
        assertEquals(List.of("foto.png"), usuario.getFotoUsuario(), "La lista de fotos debe coincidir");
        assertEquals("Hello", usuario.getPresentacion(), "La presentación debe coincidir");
        assertEquals(RoleEnum.USER, usuario.getRollUsuario(), "El rol debe coincidir");
        assertEquals(plan, usuario.getPlanUsuario(), "El plan debe coincidir");
        assertEquals(cursos, usuario.getCursosUsuario(), "La lista de cursos debe coincidir");
        assertEquals(now, usuario.getFechaRegistroUsuario(), "La fecha debe coincidir");
        assertTrue(usuario.getIsEnabled(), "El usuario debería estar habilitado");
        assertTrue(usuario.getAccountNoExpired(), "La cuenta no debería haber expirado");
        assertTrue(usuario.getAccountNoLocked(), "La cuenta no debería estar bloqueada");
        assertTrue(usuario.getCredentialsNoExpired(), "Las credenciales no deberían haber expirado");

        Usuario emptyUsuario = new Usuario();
        emptyUsuario.setIdUsuario(2L);
        assertEquals(2L, emptyUsuario.getIdUsuario(), "El ID de usuario debería ser 2");

        assertNotNull(usuario.toString(), "El toString no debería ser nulo");
    }

    /**
     * Prueba la creación y validación de campos del modelo {@link Curso}.
     */
    @Test
    void testCurso_DeberiaMapearCamposCorrectamente() {
        Usuario profe = new Usuario();
        List<Usuario> profes = List.of(profe);
        List<Clase> clases = new ArrayList<>();
        List<Plan> planes = new ArrayList<>();
        Date now = new Date();
        BigDecimal precio = new BigDecimal("99.99");

        Curso curso = Curso.builder()
                .idCurso(1L)
                .nombreCurso("Java Course")
                .profesoresCurso(profes)
                .fechaPublicacionCurso(now)
                .clasesCurso(clases)
                .planesCurso(planes)
                .descripcionCorta("Short")
                .descripcionLarga("Long")
                .imagenCurso("img.png")
                .precioCurso(precio)
                .build();

        assertEquals(1L, curso.getIdCurso(), "El ID del curso debe coincidir");
        assertEquals("Java Course", curso.getNombreCurso(), "El nombre del curso debe coincidir");
        assertEquals(profes, curso.getProfesoresCurso(), "La lista de profesores debe coincidir");
        assertEquals(now, curso.getFechaPublicacionCurso(), "La fecha debe coincidir");
        assertEquals(clases, curso.getClasesCurso(), "La lista de clases debe coincidir");
        assertEquals(planes, curso.getPlanesCurso(), "La lista de planes debe coincidir");
        assertEquals("Short", curso.getDescripcionCorta(), "La descripción corta debe coincidir");
        assertEquals("Long", curso.getDescripcionLarga(), "La descripción larga debe coincidir");
        assertEquals("img.png", curso.getImagenCurso(), "La imagen debe coincidir");
        assertEquals(precio, curso.getPrecioCurso(), "El precio debe coincidir");

        Curso emptyCurso = new Curso();
        emptyCurso.setIdCurso(2L);
        assertEquals(2L, emptyCurso.getIdCurso(), "El ID del curso debería ser 2");

        assertNotNull(curso.toString(), "El toString no debería ser nulo");
    }

    @Test
    void testClase() {
        Curso curso = new Curso();
        curso.setIdCurso(1L);

        Clase clase1 = new Clase();
        clase1.setIdClase(1L);
        clase1.setNombreClase("Intro");
        clase1.setDescripcionClase("Desc");
        clase1.setContenidoClase("Content");
        clase1.setTipoClase(0);
        clase1.setDireccionClase("/path");
        clase1.setPosicionClase(1);
        clase1.setCursoClase(curso);

        assertEquals(1L, clase1.getIdClase());
        assertEquals("Intro", clase1.getNombreClase());
        assertEquals("Desc", clase1.getDescripcionClase());
        assertEquals("Content", clase1.getContenidoClase());
        assertEquals(0, clase1.getTipoClase());
        assertEquals("/path", clase1.getDireccionClase());
        assertEquals(1, clase1.getPosicionClase());
        assertEquals(curso, clase1.getCursoClase());

        Clase clase2 = new Clase();
        clase2.setIdClase(1L);

        Clase clase3 = new Clase();
        clase3.setIdClase(2L);

        assertEquals(clase1, clase2);
        assertNotEquals(clase1, clase3);
        assertNotEquals(clase1, null);
        assertNotEquals(clase1, new Object());
        assertEquals(clase1.hashCode(), clase2.hashCode());
        assertNotNull(clase1.toString());
    }

    @Test
    void testPlan() {
        List<Curso> cursos = new ArrayList<>();
        BigDecimal precio = new BigDecimal("19.99");

        Plan plan = Plan.builder()
                .idPlan(1L)
                .nombrePlan("Premium")
                .precioPlan(precio)
                .cursosPlan(cursos)
                .build();

        assertEquals(1L, plan.getIdPlan());
        assertEquals("Premium", plan.getNombrePlan());
        assertEquals(precio, plan.getPrecioPlan());
        assertEquals(cursos, plan.getCursosPlan());

        Plan emptyPlan = new Plan();
        emptyPlan.setIdPlan(2L);
        assertEquals(2L, emptyPlan.getIdPlan());

        assertNotNull(plan.toString());
    }

    @Test
    void testLogin() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        Login login = new Login(1L, usuario, "test@test.com", "password");

        assertEquals(1L, login.getIdUsuario());
        assertEquals(usuario, login.getUsuario());
        assertEquals("test@test.com", login.getCorreoElectronico());
        assertEquals("password", login.getPassword());

        Login emptyLogin = new Login();
        emptyLogin.setIdUsuario(2L);
        assertEquals(2L, emptyLogin.getIdUsuario());

        assertNotNull(login.toString());
    }
}
