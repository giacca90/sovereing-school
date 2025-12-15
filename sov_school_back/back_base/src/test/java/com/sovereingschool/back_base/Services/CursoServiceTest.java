package com.sovereingschool.back_base.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Exceptions.ServiceException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.PlanRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    // ==========================
    // Tests createCurso()
    // ==========================
    @Nested
    class CreateCursoTests {

        @Test
        void createCurso_SuccessfulCreation() throws RepositoryException {
            Curso curso = new Curso();
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());

            // Simular que el repo asigna un ID
            Curso savedCurso = new Curso();
            savedCurso.setIdCurso(10L);
            savedCurso.setNombreCurso("curso");
            savedCurso.setDescripcionCorta("descripcion corta");
            savedCurso.setFechaPublicacionCurso(curso.getFechaPublicacionCurso());

            when(cursoRepo.save(curso)).thenReturn(savedCurso);

            Long resp = cursoService.createCurso(curso);

            assertNotNull(resp);
            assertEquals(10L, resp);

            verify(cursoRepo).save(curso);
        }

        @Test
        void createCurso_ThrowsRepositoryException_WhenSavedCursoHasNullId() {
            Curso curso = new Curso();
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());

            when(cursoRepo.save(curso)).thenThrow(new IllegalArgumentException());

            RepositoryException ex = assertThrows(RepositoryException.class,
                    () -> cursoService.createCurso(curso));

            assertTrue(ex.getMessage().contains("Error en crear el curso"));
            verify(cursoRepo).save(curso);
        }
    }

    // ==========================
    // Tests getCurso()
    // ==========================
    @Nested
    class GetCursoTests {
        @Test
        void getCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            Curso curso = new Curso();
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());

            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));

            Curso resp = cursoService.getCurso(cursoId);

            assertNotNull(resp);
            assertEquals(curso.getNombreCurso(), resp.getNombreCurso());
            assertEquals(curso.getDescripcionCorta(), resp.getDescripcionCorta());
            assertEquals(curso.getFechaPublicacionCurso(), resp.getFechaPublicacionCurso());

            verify(cursoRepo).findById(cursoId);
        }

        @Test
        void getCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getCurso(cursoId));

            assertEquals("Error en obtener el curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findById(cursoId);
        }
    }

    // ==========================
    // Tests getNombreCurso()
    // ==========================
    @Nested
    class GetNombreCursoTests {

        @Test
        void getNombreCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            String nombreCurso = "curso";
            Curso curso = new Curso();
            curso.setNombreCurso(nombreCurso);
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());

            when(cursoRepo.findNombreCursoById(cursoId)).thenReturn(Optional.of(nombreCurso));

            String resp = cursoService.getNombreCurso(cursoId);

            assertNotNull(resp);
            assertEquals(nombreCurso, resp);

            verify(cursoRepo).findNombreCursoById(cursoId);
        }

        @Test
        void getNombreCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findNombreCursoById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getNombreCurso(cursoId));

            assertEquals("Error en obtener el nombre del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findNombreCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getProfesoresCurso()
    // ==========================
    @Nested
    class GetProfesoresCursoTests {
        @Test
        void getProfesoresCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            List<Usuario> profesores = List.of(new Usuario(), new Usuario());
            Curso curso = new Curso();
            curso.setProfesoresCurso(profesores);

            when(cursoRepo.findProfesoresCursoById(cursoId)).thenReturn(profesores);

            List<Usuario> resp = cursoService.getProfesoresCurso(cursoId);

            assertNotNull(resp);
            assertEquals(profesores, resp);

            verify(cursoRepo).findProfesoresCursoById(cursoId);
        }

        @Test
        void getProfesoresCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findProfesoresCursoById(cursoId)).thenReturn(null);

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getProfesoresCurso(cursoId));

            assertEquals("Error en obtener los profesores del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findProfesoresCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getFechaCreacionCurso()
    // ==========================
    @Nested
    class GetFechaCreacionCursoTests {

        @Test
        void getFechaCreacionCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            Date fechaCreacionCurso = new Date();
            Curso curso = new Curso();
            curso.setFechaPublicacionCurso(fechaCreacionCurso);

            when(cursoRepo.findFechaCreacionCursoById(cursoId)).thenReturn(Optional.of(fechaCreacionCurso));

            Date resp = cursoService.getFechaCreacionCurso(cursoId);

            assertNotNull(resp);
            assertEquals(fechaCreacionCurso, resp);

            verify(cursoRepo).findFechaCreacionCursoById(cursoId);
        }

        @Test
        void getFechaCreacionCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findFechaCreacionCursoById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getFechaCreacionCurso(cursoId));

            assertEquals("Error en obtener la fecha de creación del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findFechaCreacionCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getClasesDelCurso()
    // ==========================
    @Nested
    class GetClasesDelCursoTests {

        @Test
        void getClasesDelCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setClasesCurso(clases);

            when(cursoRepo.findClasesCursoById(cursoId)).thenReturn(clases);

            List<Clase> resp = cursoService.getClasesDelCurso(cursoId);

            assertNotNull(resp);
            assertEquals(clases, resp);

            verify(cursoRepo).findClasesCursoById(cursoId);
        }

        @Test
        void getClasesDelCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findClasesCursoById(cursoId)).thenReturn(null);

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getClasesDelCurso(cursoId));

            assertEquals("Error en obtener las clases del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findClasesCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getPlanesDelCurso()
    // ==========================
    @Nested
    class GetPlanesDelCursoTests {

        @Test
        void getPlanesDelCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            List<Plan> planes = List.of(new Plan(), new Plan());
            Curso curso = new Curso();
            curso.setPlanesCurso(planes);

            when(cursoRepo.findPlanesCursoById(cursoId)).thenReturn(planes);

            List<Plan> resp = cursoService.getPlanesDelCurso(cursoId);

            assertNotNull(resp);
            assertEquals(planes, resp);

            verify(cursoRepo).findPlanesCursoById(cursoId);
        }

        @Test
        void getPlanesDelCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findPlanesCursoById(cursoId)).thenReturn(null);

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getPlanesDelCurso(cursoId));

            assertEquals("Error en obtener los planes del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findPlanesCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getPrecioCurso()
    // ==========================
    @Nested
    class GetPrecioCursoTests {

        @Test
        void getPrecioCurso_SuccessfulRetrieval() throws NotFoundException {
            Long cursoId = 1L;
            BigDecimal precioCurso = new BigDecimal(100);
            Curso curso = new Curso();
            curso.setPrecioCurso(precioCurso);

            when(cursoRepo.findPrecioCursoById(cursoId)).thenReturn(Optional.of(precioCurso));

            BigDecimal resp = cursoService.getPrecioCurso(cursoId);

            assertNotNull(resp);
            assertEquals(precioCurso, resp);

            verify(cursoRepo).findPrecioCursoById(cursoId);
        }

        @Test
        void getPrecioCurso_Error() {
            Long cursoId = 1L;

            // Simular que el curso no existe
            when(cursoRepo.findPrecioCursoById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getPrecioCurso(cursoId));

            assertEquals("Error en obtener el precio del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findPrecioCursoById(cursoId);
        }
    }

    // ==========================
    // Tests updateCurso()
    // ==========================
    @Nested
    class UpdateCursoTests {
        @Test
        void updateCurso_NewCurso_SuccessfulUpdate()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {
            Long cursoId = 0L;
            Clase clase1 = new Clase();
            clase1.setIdClase(0L);
            Clase clase2 = new Clase();
            clase2.setIdClase(0L);
            List<Clase> clases = List.of(clase1, clase2);
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            Curso cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            when(claseRepo.save(any(Clase.class))).thenReturn(new Clase());

            // Act
            Curso resp = spyService.updateCurso(curso);

            // Assert
            assertNotNull(resp);
            assertEquals(cursoGuardado, resp);
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_OldCurso_SuccessfulUpdate()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {
            Long cursoId = 10L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            doNothing().when(spyService).creaCarpetaCurso(curso);

            // Act
            Curso resp = spyService.updateCurso(curso);

            // Assert
            assertNotNull(resp);
            assertEquals(curso, resp);
            verify(spyService).creaCarpetaCurso(curso);
            verify(spyService).creaClasesCurso(curso, clases);
            verify(spyService).actualizarChatCurso(curso);
            verify(spyService).actualizarStreamCurso(curso);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_NewCurso_ErrorCrearCarpeta()
                throws InternalServerException {
            Long cursoId = 0L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            Curso cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            tempDir.toFile().setWritable(false);

            // Act
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error en crear la carpeta del curso."));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
        }

        @Test
        void updateCurso_NewCurso_ErrorCrearClasesCurso()
                throws InternalServerException, RepositoryException, NotFoundException {
            Long cursoId = 0L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            Curso cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            doNothing().when(spyService).creaCarpetaCurso(cursoGuardado);
            doThrow(new InternalServerException("Error")).when(spyService).creaClasesCurso(cursoGuardado, clases);

            // Act
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
        }

        @Test
        void updateCurso_NewCurso_ErrorActualizarChatCurso()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {
            Long cursoId = 0L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            Curso cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            doNothing().when(spyService).creaCarpetaCurso(cursoGuardado);
            doThrow(new InternalComunicationException("Error")).when(spyService).actualizarChatCurso(cursoGuardado);

            // Act
            InternalComunicationException ex = assertThrows(InternalComunicationException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
            verify(spyService).actualizarChatCurso(cursoGuardado);
        }

        @Test
        void updateCurso_NewCurso_ErrorActualizarStreamCurso()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {
            Long cursoId = 0L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            Curso cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            doNothing().when(spyService).creaCarpetaCurso(cursoGuardado);
            doThrow(new InternalComunicationException("Error")).when(spyService).actualizarStreamCurso(cursoGuardado);

            // Act
            InternalComunicationException ex = assertThrows(InternalComunicationException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
        }

        @Test
        void updateCurso_NewCurso_ErrorUpdateSSR()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {
            Long cursoId = 0L;
            List<Clase> clases = List.of(new Clase(), new Clase());
            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            Curso cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clases);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            doNothing().when(spyService).creaCarpetaCurso(cursoGuardado);
            doThrow(new InternalComunicationException("Error")).when(spyService).updateSSR();

            // Act
            InternalComunicationException ex = assertThrows(InternalComunicationException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();
        }
    }

    // ==========================
    // Tests deleteCurso()
    // ==========================
    @Nested
    class DeleteCursoTests {

        @Test
        void deleteCurso_SuccessfulDeletion() throws ServiceException {
            Long cursoId = 1L;

            // Preparar curso con clases
            Clase clase1 = new Clase();
            clase1.setIdClase(10L);
            Clase clase2 = new Clase();
            clase2.setIdClase(20L);

            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setClasesCurso(List.of(clase1, clase2));

            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase()
            doNothing().when(spyService).deleteClase(any(Clase.class));

            // Act
            Boolean resp = spyService.deleteCurso(cursoId);

            // Assert
            assertTrue(resp);

            verify(spyService).getCurso(cursoId);
            verify(spyService, times(2)).deleteClase(any(Clase.class));
            verify(spyService).deleteCarpetaCurso(cursoId);
            verify(spyService).deleteCursoStream(cursoId);
            verify(spyService).deleteCursoChat(cursoId);
            verify(spyService).updateSSR();

            // El repositorio debe borrar el curso
            verify(cursoRepo).deleteById(cursoId);
        }

        @Test
        void deleteCurso_ErrorCursoNoEncontrado() throws ServiceException {
            Long cursoId = 1L;

            // Preparar curso con clases
            Clase clase1 = new Clase();
            clase1.setIdClase(10L);
            Clase clase2 = new Clase();
            clase2.setIdClase(20L);

            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setClasesCurso(List.of(clase1, clase2));

            // Spy
            CursoService spyService = spy(cursoService);

            // Act
            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert
            assertTrue(ex.getMessage().contains("Error borrar las clases del curso con ID "));

            verify(spyService).getCurso(cursoId);
        }

        @Test
        void deleteCurso_ErrorDeleteClase() throws ServiceException {
            Long cursoId = 1L;

            // Preparar curso con clases
            Clase clase1 = new Clase();
            clase1.setIdClase(10L);
            Clase clase2 = new Clase();
            clase2.setIdClase(20L);

            Curso curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setClasesCurso(List.of(clase1, clase2));

            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase() debe lanzar excepción
            doThrow(new ServiceException("Error en borrar la clase")).when(spyService).deleteClase(any(Clase.class));

            // Act
            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert

            assertTrue(ex.getMessage().contains("Error borrar las clases del curso con ID "));
            verify(spyService).getCurso(cursoId);
        }

    }

    // ==========================
    // Tests getAll()
    // ==========================
    @Nested
    class GetAllTests {
    }

    // ==========================
    // Tests deleteClase()
    // ==========================
    @Nested
    class DeleteClaseTests {
    }

    // ==========================
    // Tests subeVideo()
    // ==========================
    @Nested
    class SubeVideoTests {
    }

    @Mock
    private WebClientConfig webClientConfig;

    // Usamos RETURNS_DEEP_STUBS para evitar tener que mockear cada etapa
    // manualmente
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private PlanRepository planRepo;
    @Mock
    private InitAppService initAppService;

    private CursoService cursoService;
    private final String backStreamURL = "http://mocked-stream-url";
    private final String backChatURL = "http://mocked-chat-url";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SSLException, URISyntaxException {
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);
        cursoService = new CursoService(
                tempDir.toString(),
                backStreamURL,
                backChatURL,
                cursoRepo,
                claseRepo,
                usuarioRepo,
                planRepo,
                initAppService,
                webClientConfig);
    }
}