package com.sovereingschool.back_streaming.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_streaming.Models.StatusClase;
import com.sovereingschool.back_streaming.Models.StatusCurso;
import com.sovereingschool.back_streaming.Models.UsuarioCursos;
import com.sovereingschool.back_streaming.Repositories.UsuarioCursosRepository;

/**
 * Pruebas unitarias para {@link UsuarioCursosService}.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioCursosServiceTest {

    @Nested
    class SyncUserCoursesTests {
        /**
         * Prueba la sincronización exitosa de los cursos de usuario.
         */
        @Test
        void syncUserCourses_ShouldSyncSuccessfully() {
            final Usuario user = new Usuario();
            user.setIdUsuario(1L);
            user.setRollUsuario(RoleEnum.USER);
            final Curso curso = new Curso();
            curso.setIdCurso(10L);
            final Clase clase = new Clase();
            clase.setIdClase(100L);
            clase.setDireccionClase("/path/to/clase.m3u8");
            curso.setClasesCurso(List.of(clase));
            user.setCursosUsuario(List.of(curso));

            when(usuarioRepository.findAll()).thenReturn(List.of(user));
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());

            final Path mockPath = mock(Path.class);
            final Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.list(any(Path.class))).thenReturn(Stream.empty());

            usuarioCursosService.syncUserCourses();
            verify(usuarioCursosRepository, times(1)).save(any(UsuarioCursos.class));
        }

        /**
         * Prueba que se omite la sincronización si el usuario ya tiene cursos
         * registrados.
         */
        @Test
        void syncUserCourses_WhenUserAlreadyHasCourses_ShouldSkip() {
            final Usuario user = new Usuario();
            user.setIdUsuario(1L);
            user.setRollUsuario(RoleEnum.USER);
            final Curso curso = new Curso();
            curso.setIdCurso(10L);
            final Clase clase = new Clase();
            clase.setIdClase(100L);
            clase.setDireccionClase("/path/to/clase.m3u8");
            curso.setClasesCurso(List.of(clase));
            user.setCursosUsuario(List.of(curso));

            final UsuarioCursos existingUc = new UsuarioCursos();
            existingUc.setIdUsuario(1L);

            when(usuarioRepository.findAll()).thenReturn(List.of(user));
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(existingUc));

            usuarioCursosService.syncUserCourses();
            verify(usuarioCursosRepository, times(0)).save(any(UsuarioCursos.class));
        }

        /**
         * Prueba la sincronización cuando no se encuentra la ruta del archivo.
         */
        @Test
        void testSyncUserCourses_PathNotFound() {
            Usuario user = new Usuario();
            user.setIdUsuario(1L);
            user.setRollUsuario(RoleEnum.USER);
            Curso curso = new Curso();
            curso.setIdCurso(10L);
            Clase clase = new Clase();
            clase.setIdClase(100L);
            clase.setDireccionClase("/path/to/clase.m3u8");
            curso.setClasesCurso(List.of(clase));
            user.setCursosUsuario(List.of(curso));

            when(usuarioRepository.findAll()).thenReturn(List.of(user));
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            usuarioCursosService.syncUserCourses();
            verify(usuarioCursosRepository, times(1)).save(any(UsuarioCursos.class));
        }
    }

    @Nested
    class SyncUserCoursesMoreTests {
        @Test
        void testSyncUserCourses_LoopCoverage() {
            Usuario user1 = new Usuario();
            user1.setIdUsuario(1L);
            user1.setRollUsuario(RoleEnum.USER);
            user1.setCursosUsuario(new ArrayList<>());

            Usuario user2 = new Usuario();
            user2.setIdUsuario(2L);
            user2.setRollUsuario(RoleEnum.USER);
            user2.setCursosUsuario(new ArrayList<>());

            when(usuarioRepository.findAll()).thenReturn(List.of(user1, user2));
            when(usuarioCursosRepository.findByIdUsuario(anyLong())).thenReturn(Optional.empty());

            usuarioCursosService.syncUserCourses();
            verify(usuarioCursosRepository, times(2)).save(any(UsuarioCursos.class));
        }
    }

    @Nested
    class AddNuevoUsuarioTests {
        /**
         * Prueba la adición exitosa de un nuevo usuario.
         */
        @Test
        void testAddNuevoUsuario_New() throws InternalServerException {
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setRollUsuario(RoleEnum.USER);
            usuario.setCursosUsuario(new ArrayList<>());
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());
            when(usuarioCursosRepository.save(any(UsuarioCursos.class))).thenAnswer(i -> i.getArguments()[0]);
            String result = usuarioCursosService.addNuevoUsuario(usuario);
            assertEquals("Nuevo Usuario Insertado con Exito!!!", result);
        }

        /**
         * Prueba la adición de un usuario que ya existe.
         */
        @Test
        void testAddNuevoUsuario_Existing() throws InternalServerException {
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setRollUsuario(RoleEnum.USER);
            usuario.setCursosUsuario(new ArrayList<>());
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(new UsuarioCursos()));

            String result = usuarioCursosService.addNuevoUsuario(usuario);
            assertEquals("Nuevo Usuario Insertado con Exito!!!", result);
        }
    }

    @Nested
    class GetClaseTests {
        /**
         * Prueba la obtención exitosa de una clase.
         */
        @Test
        void testGetClase_Success() throws InternalServerException {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            StatusClase scl = new StatusClase();
            scl.setIdClase(10L);
            sc.setClases(List.of(scl));
            uc.setCursos(List.of(sc));
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));
            Clase clase = new Clase();
            clase.setDireccionClase("url");
            when(claseRepository.findById(10L)).thenReturn(Optional.of(clase));
            String result = usuarioCursosService.getClase(1L, 1L, 10L);
            assertEquals("url", result);
        }

        /**
         * Prueba la obtención exitosa de una clase para un administrador.
         */
        @Test
        void testGetClase_AdminSuccess() throws InternalServerException {
            Clase clase = new Clase();
            clase.setDireccionClase("admin-url");
            when(claseRepository.findById(10L)).thenReturn(Optional.of(clase));

            // For ADMIN, it shouldn't check UsuarioCursosRepository
            // We just need to make sure the logic handles RoleEnum.ADMIN
            // Wait, getClase takes userId, cursoId, claseId.
            // If user is ADMIN, it just returns the class URL.
            // But how does it know the user is ADMIN?
            // Looking at UsuarioCursosService.java:
            // it checks usuarioCursosRepository.findByIdUsuario(userId)
            // and then checks uc.getRolUsuario() == RoleEnum.ADMIN

            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.ADMIN);
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));

            String result = usuarioCursosService.getClase(1L, 1L, 10L);
            assertEquals("admin-url", result);
        }

        /**
         * Prueba el error cuando no se encuentra el usuario al obtener una clase.
         */
        @Test
        void testGetClase_UserNotFound() {
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                usuarioCursosService.getClase(1L, 1L, 10L);
            });
        }

        /**
         * Prueba el error cuando no se encuentra el curso al obtener una clase.
         */
        @Test
        void testGetClase_CursoNotFound() {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            uc.setCursos(new ArrayList<>());
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));

            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                usuarioCursosService.getClase(1L, 1L, 10L);
            });
        }

        /**
         * Prueba el error cuando la clase no se encuentra en el curso.
         */
        @Test
        void testGetClase_ClaseNotFoundInCurso() {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            sc.setClases(new ArrayList<>());
            uc.setCursos(List.of(sc));
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));

            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                usuarioCursosService.getClase(1L, 1L, 10L);
            });
        }

        /**
         * Prueba el error cuando la clase no existe en el repositorio.
         */
        @Test
        void testGetClase_ClaseNotFoundInRepo() {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            StatusClase scl = new StatusClase();
            scl.setIdClase(10L);
            sc.setClases(List.of(scl));
            uc.setCursos(List.of(sc));
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));
            when(claseRepository.findById(10L)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                usuarioCursosService.getClase(1L, 1L, 10L);
            });
        }
    }

    @Nested
    class AddClaseTests {
        /**
         * Prueba la adición exitosa de una clase.
         */
        @Test
        void testAddClase_Success() {
            UsuarioCursos uc = new UsuarioCursos();
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            sc.setClases(new ArrayList<>());
            uc.setCursos(List.of(sc));
            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(List.of(uc));
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.list(any(Path.class))).thenReturn(Stream.empty());

            boolean result = usuarioCursosService.addClase(1L, clase);
            assertTrue(result);
            verify(mongoTemplate).save(uc);
        }

        /**
         * Prueba el error al añadir una clase cuando el usuario no existe.
         */
        @Test
        void testAddClase_UserNotFound() {
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);

            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(new ArrayList<>());

            boolean result = usuarioCursosService.addClase(1L, clase);
            assertTrue(!result);
        }

        /**
         * Prueba el error al añadir una clase cuando el curso no existe.
         */
        @Test
        void testAddClase_CursoNotFound() {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setCursos(new ArrayList<>());

            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);

            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(List.of(uc));

            boolean result = usuarioCursosService.addClase(1L, clase);
            assertTrue(!result);
        }

        /**
         * Prueba el error al añadir una clase cuando no se encuentra la ruta del
         * archivo.
         */
        @Test
        void testAddClase_PathNotFound() {
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            boolean result = usuarioCursosService.addClase(1L, clase);
            assertTrue(!result);
        }
    }

    @Nested
    class DeleteClaseTests {
        /**
         * Prueba la eliminación exitosa de una clase.
         */
        @Test
        void testDeleteClase_Success() {
            UsuarioCursos uc = new UsuarioCursos();
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            StatusClase scl = new StatusClase();
            scl.setIdClase(10L);
            sc.setClases(new ArrayList<>(List.of(scl)));
            uc.setCursos(List.of(sc));

            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(List.of(uc));

            boolean result = usuarioCursosService.deleteClase(1L, 10L);
            assertTrue(result);
            verify(mongoTemplate).save(uc);
            assertTrue(sc.getClases().isEmpty());
        }

        /**
         * Prueba el error al eliminar una clase cuando el usuario no existe.
         */
        @Test
        void testDeleteClase_UserNotFound() {
            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(new ArrayList<>());
            boolean result = usuarioCursosService.deleteClase(1L, 10L);
            assertTrue(!result);
        }
    }

    @Nested
    class GetStatusTests {
        /**
         * Prueba la obtención exitosa del estado del curso para un administrador.
         */
        @Test
        void testGetStatus_AdminSuccess() throws InternalServerException {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.ADMIN);
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));

            Curso curso = new Curso();
            curso.setIdCurso(1L);
            Clase clase = new Clase();
            clase.setIdClase(10L);
            curso.setClasesCurso(List.of(clase));
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));

            Long result = usuarioCursosService.getStatus(1L, 1L);
            assertEquals(10L, result);
        }

        /**
         * Prueba la obtención del estado del curso para un usuario.
         */
        @Test
        void testGetStatus_UserSuccess_NextClass() throws InternalServerException {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            StatusClase scl1 = new StatusClase();
            scl1.setIdClase(10L);
            scl1.setCompleted(true);
            StatusClase scl2 = new StatusClase();
            scl2.setIdClase(20L);
            scl2.setCompleted(false);
            sc.setClases(List.of(scl1, scl2));
            uc.setCursos(List.of(sc));

            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));

            Long result = usuarioCursosService.getStatus(1L, 1L);
            assertEquals(20L, result);
        }

        /**
         * Prueba la obtención del estado del curso cuando todas las clases están
         * completadas.
         */
        @Test
        void testGetStatus_UserSuccess_AllCompleted() throws InternalServerException {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            StatusClase scl1 = new StatusClase();
            scl1.setIdClase(10L);
            scl1.setCompleted(true);
            sc.setClases(List.of(scl1));
            uc.setCursos(List.of(sc));

            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));

            Long result = usuarioCursosService.getStatus(1L, 1L);
            assertEquals(10L, result);
        }

        /**
         * Prueba el error cuando no se encuentra el usuario al consultar el estado.
         */
        @Test
        void testGetStatus_UserNotFound() {
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());
            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                usuarioCursosService.getStatus(1L, 1L);
            });
        }
    }

    @Nested
    class DeleteUsuarioCursosTests {
        /**
         * Prueba la eliminación exitosa de los cursos de un usuario.
         */
        @Test
        void testDeleteUsuarioCursos_Success() {
            UsuarioCursos uc = new UsuarioCursos();
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));
            boolean result = usuarioCursosService.deleteUsuarioCursos(1L);
            assertTrue(result);
            verify(usuarioCursosRepository).delete(uc);
        }

        @Test
        void testDeleteUsuarioCursos_NotFound() {
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());
            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                usuarioCursosService.deleteUsuarioCursos(1L);
            });
        }
    }

    @Nested
    class DeleteCursoTests {
        @Test
        void testDeleteCurso_Success() {
            UsuarioCursos uc = new UsuarioCursos();
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            uc.setCursos(new ArrayList<>(List.of(sc)));
            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(List.of(uc));
            boolean result = usuarioCursosService.deleteCurso(1L);
            assertTrue(result);
            verify(usuarioCursosRepository).save(uc);
        }

        @Test
        void testDeleteCurso_NotFound() {
            when(mongoTemplate.find(any(Query.class), eq(UsuarioCursos.class))).thenReturn(new ArrayList<>());
            boolean result = usuarioCursosService.deleteCurso(1L);
            assertTrue(!result);
        }
    }

    @Nested
    class UpdateCursosUsuarioTests {
        @Test
        void testUpdateCursosUsuario() {
            Usuario user = new Usuario();
            Curso curso = new Curso();
            curso.setIdCurso(1L);
            user.setCursosUsuario(List.of(curso));
            UsuarioCursos uc = new UsuarioCursos();
            uc.setCursos(new ArrayList<>());
            when(cursoRepository.findClasesCursoById(1L)).thenReturn(new ArrayList<>());
            usuarioCursosService.updateCursosUsuario(user, uc);
            verify(usuarioCursosRepository).save(uc);
            assertEquals(1, uc.getCursos().size());
        }
    }

    @Nested
    class ActualizarStatusUsuarioTests {
        @Test
        void testActualizarStatusUsuario() {
            UsuarioCursos uc = new UsuarioCursos();
            StatusCurso sc = new StatusCurso();
            sc.setIdCurso(1L);
            sc.setClases(new ArrayList<>());
            uc.setCursos(List.of(sc));
            Curso curso = new Curso();
            curso.setIdCurso(1L);
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");
            curso.setClasesCurso(List.of(clase));

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.list(any(Path.class))).thenReturn(Stream.empty());

            usuarioCursosService.actualizarStatusUsuario(uc, curso);
            assertEquals(1, sc.getClases().size());
        }
    }

    @Nested
    class GetTotalSegmentsTests {
        @Test
        void testGetTotalSegments_Success() {
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            Path mockResolutionDir = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);

            // Mock subdirs list
            mockedFiles.when(() -> Files.list(eq(mockParent))).thenReturn(Stream.of(mockResolutionDir));
            // Mock segments list
            mockedFiles.when(() -> Files.list(eq(mockResolutionDir)))
                    .thenReturn(Stream.of(mock(Path.class), mock(Path.class), mock(Path.class)));
            // Mock Files.isDirectory to return true for resolutionDir
            mockedFiles.when(() -> Files.isDirectory(eq(mockResolutionDir))).thenReturn(true);

            int total = usuarioCursosService.getTotalSegments(clase);
            assertEquals(2, total);
        }

        @Test
        void testGetTotalSegments_IOException() {
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);

            // Mock subdirs list to throw IOException
            mockedFiles.when(() -> Files.list(eq(mockParent))).thenThrow(new java.io.IOException());

            int total = usuarioCursosService.getTotalSegments(clase);
            assertEquals(0, total);
        }

        @Test
        void testGetTotalSegments_NoResolutionDir() {
            Clase clase = new Clase();
            clase.setIdClase(10L);
            clase.setDireccionClase("/path/video.m3u8");

            Path mockPath = mock(Path.class);
            Path mockParent = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getParent()).thenReturn(mockParent);
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);

            // Mock subdirs list
            mockedFiles.when(() -> Files.list(eq(mockParent))).thenReturn(Stream.empty());

            int total = usuarioCursosService.getTotalSegments(clase);
            assertEquals(0, total);
        }
    }

    @Nested
    class ComplexCoverageTests {
        @Test
        void testGetStatus_UserNoCursos() throws InternalServerException {
            UsuarioCursos uc = new UsuarioCursos();
            uc.setRolUsuario(RoleEnum.USER);
            uc.setCursos(new ArrayList<>());
            when(usuarioCursosRepository.findByIdUsuario(1L)).thenReturn(Optional.of(uc));
            Long result = usuarioCursosService.getStatus(1L, 1L);
            assertEquals(0L, result);
        }

        @Test
        void testActualizarCursoStream_EmptyUsers() throws InternalServerException {
            Curso curso = new Curso();
            curso.setIdCurso(1L);
            when(usuarioCursosRepository.findAllByIdCurso(1L)).thenReturn(new ArrayList<>());
            usuarioCursosService.actualizarCursoStream(curso);
            verify(usuarioCursosRepository, times(0)).saveAll(any());
        }

        @Test
        void testProcesarVideosAsync_Exception() throws NotFoundException {
            Curso curso = new Curso();
            curso.setIdCurso(1L);
            curso.setClasesCurso(List.of(new Clase()));
            doThrow(new NotFoundException("Not found")).when(streamingService).convertVideos(any());
            org.junit.jupiter.api.Assertions.assertThrows(InternalServerException.class, () -> {
                usuarioCursosService.procesarVideosAsync(curso);
            });
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted(); // Clear interrupt status
        }
    }

    @Mock
    private StreamingService streamingService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private UsuarioCursosRepository usuarioCursosRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    private UsuarioCursosService usuarioCursosService;

    private MockedStatic<Paths> mockedPaths;
    private MockedStatic<Files> mockedFiles;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        usuarioCursosService = new UsuarioCursosService(streamingService, usuarioRepository, cursoRepository,
                claseRepository, usuarioCursosRepository, mongoTemplate);
        mockedPaths = mockStatic(Paths.class);
        mockedFiles = mockStatic(Files.class);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        mockedPaths.close();
        mockedFiles.close();
    }
}
