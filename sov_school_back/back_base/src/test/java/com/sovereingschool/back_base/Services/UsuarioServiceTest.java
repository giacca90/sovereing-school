package com.sovereingschool.back_base.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.CursosUsuario;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    // ==========================
    // Tests createUsuario()
    // ==========================
    @Nested
    class CreateUsuarioTests {

        @Test
        void createUsuario_SuccessfulCreation() throws RepositoryException, InternalComunicationException {
            NewUsuario newUsuario = new NewUsuario(
                    "pepito",
                    "pepito@example.com",
                    "plainpass",
                    null,
                    null,
                    new ArrayList<>(),
                    new Date());

            Usuario usuarioInsertado = new Usuario();
            usuarioInsertado.setIdUsuario(10L);
            usuarioInsertado.setNombreUsuario("pepito");
            usuarioInsertado.setRollUsuario(RoleEnum.USER);
            usuarioInsertado.setIsEnabled(true);
            usuarioInsertado.setAccountNoExpired(true);
            usuarioInsertado.setCredentialsNoExpired(true);
            usuarioInsertado.setAccountNoLocked(true);

            when(passwordEncoder.encode("plainpass")).thenReturn("encodedPass");
            when(usuarioRepo.save(any(Usuario.class))).thenReturn(usuarioInsertado);
            when(loginRepo.save(any(Login.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken(any(Authentication.class), eq("access"), eq(10L))).thenReturn("access-token");
            when(jwtUtil.generateToken(any(Authentication.class), eq("refresh"), eq(10L))).thenReturn("refresh-token");

            UsuarioService spyService = spy(usuarioService);
            // doNothing().when(spyService).updateSSR();

            AuthResponse resp = spyService.createUsuario(newUsuario);

            assertNotNull(resp);
            assertTrue(resp.status());
            assertEquals("Usuario creado con éxito", resp.message());
            assertEquals(usuarioInsertado, resp.usuario());
            assertEquals("access-token", resp.accessToken());
            assertEquals("refresh-token", resp.refreshToken());

            verify(usuarioRepo).save(any(Usuario.class));
            verify(loginRepo).save(any(Login.class));
            verify(passwordEncoder).encode("plainpass");
            verify(jwtUtil).generateToken(any(Authentication.class), eq("access"), eq(10L));
            verify(jwtUtil).generateToken(any(Authentication.class), eq("refresh"), eq(10L));
            verify(spyService).updateSSR();
        }

        @Test
        void createUsuario_ThrowsRepositoryException_WhenSavedUserHasNullId() {
            NewUsuario newUsuario = new NewUsuario(
                    "fallo",
                    "fallo@example.com",
                    "x",
                    null,
                    null,
                    new ArrayList<>(),
                    new Date());

            Usuario usuarioSinId = new Usuario();
            usuarioSinId.setIdUsuario(null);
            when(usuarioRepo.save(any(Usuario.class))).thenReturn(usuarioSinId);

            RepositoryException ex = assertThrows(RepositoryException.class,
                    () -> usuarioService.createUsuario(newUsuario));
            assertTrue(ex.getMessage().contains("Error al crear el usuario"));
            verify(loginRepo, never()).save(any(Login.class));
            verify(jwtUtil, never()).generateToken(any(), anyString(), any());
        }

        @Test
        void createUsuario_PropagatesDataIntegrityViolationException() {
            NewUsuario newUsuario = new NewUsuario(
                    "dup",
                    "dup@example.com",
                    "x",
                    null,
                    null,
                    new ArrayList<>(),
                    new Date());

            when(usuarioRepo.save(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

            assertThrows(DataIntegrityViolationException.class, () -> usuarioService.createUsuario(newUsuario));
            verify(loginRepo, never()).save(any(Login.class));
            verify(jwtUtil, never()).generateToken(any(), anyString(), any());
        }

        @Test
        void createUsuario_WithFotoUsuario_AssignsCorrectly()
                throws RepositoryException, InternalComunicationException {
            NewUsuario newUsuario = new NewUsuario(
                    "pepito",
                    "pepito@example.com",
                    "plainpass",
                    List.of("foto1.png", "foto2.png"),
                    null,
                    new ArrayList<>(),
                    new Date());

            Usuario usuarioInsertado = new Usuario();
            usuarioInsertado.setIdUsuario(10L);
            usuarioInsertado.setNombreUsuario("pepito");
            usuarioInsertado.setRollUsuario(RoleEnum.USER);
            usuarioInsertado.setIsEnabled(true);
            usuarioInsertado.setAccountNoExpired(true);
            usuarioInsertado.setCredentialsNoExpired(true);
            usuarioInsertado.setAccountNoLocked(true);
            usuarioInsertado.setFotoUsuario(newUsuario.fotoUsuario());

            when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
            when(usuarioRepo.save(any(Usuario.class))).thenReturn(usuarioInsertado);
            when(loginRepo.save(any(Login.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken(any(), eq("access"), eq(10L))).thenReturn("access-token");
            when(jwtUtil.generateToken(any(), eq("refresh"), eq(10L))).thenReturn("refresh-token");

            UsuarioService spyService = spy(usuarioService);
            doNothing().when(spyService).updateSSR();

            AuthResponse resp = spyService.createUsuario(newUsuario);

            assertNotNull(resp);
            assertTrue(resp.status());
            assertEquals(usuarioInsertado, resp.usuario());
            assertEquals(newUsuario.fotoUsuario(), resp.usuario().getFotoUsuario());
        }

        @Test
        void createUsuario_WithEmptyFotoUsuario_AssignsEmptyList()
                throws RepositoryException, InternalComunicationException {
            NewUsuario newUsuario = new NewUsuario(
                    "pepito",
                    "pepito@example.com",
                    "plainpass",
                    new ArrayList<>(),
                    null,
                    new ArrayList<>(),
                    new Date());

            Usuario usuarioInsertado = new Usuario();
            usuarioInsertado.setIdUsuario(10L);
            usuarioInsertado.setNombreUsuario("pepito");
            usuarioInsertado.setRollUsuario(RoleEnum.USER);
            usuarioInsertado.setIsEnabled(true);
            usuarioInsertado.setAccountNoExpired(true);
            usuarioInsertado.setCredentialsNoExpired(true);
            usuarioInsertado.setAccountNoLocked(true);
            usuarioInsertado.setFotoUsuario(newUsuario.fotoUsuario());

            when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
            when(usuarioRepo.save(any(Usuario.class))).thenReturn(usuarioInsertado);
            when(loginRepo.save(any(Login.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken(any(), eq("access"), eq(10L))).thenReturn("access-token");
            when(jwtUtil.generateToken(any(), eq("refresh"), eq(10L))).thenReturn("refresh-token");

            UsuarioService spyService = spy(usuarioService);
            doNothing().when(spyService).updateSSR();

            AuthResponse resp = spyService.createUsuario(newUsuario);

            assertNotNull(resp);
            assertTrue(resp.status());
            assertEquals(usuarioInsertado, resp.usuario());
            assertEquals(newUsuario.fotoUsuario(), resp.usuario().getFotoUsuario());
        }
    }

    // ===================
    // Tests getUsuario()
    // ===================
    @Nested
    class GetUsuarioTests {
        @Test
        void getUsuario_SuccessfulRetrieval() {
            Long userId = 1L;
            Usuario usuario = new Usuario();
            usuario.setNombreUsuario("pepito");
            usuario.setIdUsuario(userId);
            usuario.setIsEnabled(true);
            usuario.setAccountNoExpired(true);
            usuario.setPresentacion("hola");

            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.of(usuario));

            Usuario resp = usuarioService.getUsuario(userId);

            assertNotNull(resp);
            assertEquals(usuario.getIdUsuario(), resp.getIdUsuario());
            assertEquals(usuario.getNombreUsuario(), resp.getNombreUsuario());
            assertEquals(usuario.getIsEnabled(), resp.getIsEnabled());
            assertEquals(usuario.getAccountNoExpired(), resp.getAccountNoExpired());
            assertEquals(usuario.getPresentacion(), resp.getPresentacion());

        }

        @Test
        void getUsuario_Error() {
            Long userId = 1L;

            // Simular que el usuario no existe
            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class,
                    () -> usuarioService.getUsuario(userId));

            assertEquals("Error en obtener el usuario con ID " + userId, thrown.getMessage());

            verify(usuarioRepo).findUsuarioForId(userId);
        }
    }

    // ==========================
    // Tests getNombreUsuario()
    // ==========================
    @Nested
    class GetNombreUsuarioTests {
        @Test
        void getNombreUsuario_SuccessfulRetrieval() {
            Long userId = 1L;
            String nombreUsuario = "pepito";
            Usuario usuario = new Usuario();
            usuario.setNombreUsuario(nombreUsuario);
            usuario.setIdUsuario(userId);

            when(usuarioRepo.findNombreUsuarioForId(userId)).thenReturn(Optional.of(nombreUsuario));

            String resp = usuarioService.getNombreUsuario(userId);

            assertNotNull(resp);
            assertEquals(nombreUsuario, resp);

            verify(usuarioRepo).findNombreUsuarioForId(userId);
        }

        @Test
        void getNombreUsuario_Error() {
            Long userId = 1L;

            // Simular que el usuario no existe
            when(usuarioRepo.findNombreUsuarioForId(userId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class,
                    () -> usuarioService.getNombreUsuario(userId));

            assertEquals("Error en obtener el nombre del usuario con ID " + userId, thrown.getMessage());

            verify(usuarioRepo).findNombreUsuarioForId(userId);
        }
    }

    // ==========================
    // Tests getFotosUsuario()
    // ==========================
    @Nested
    class GetFotosUsuarioTests {
        @Test
        void getFotosUsuario_SuccessfulRetrieval() {
            Long userId = 1L;
            List<String> fotosUsuario = List.of("foto1.png", "foto2.png");
            Usuario usuario = new Usuario();
            usuario.setFotoUsuario(fotosUsuario);
            usuario.setIdUsuario(userId);

            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.of(usuario));

            List<String> resp = usuarioService.getFotosUsuario(userId);

            assertNotNull(resp);
            assertEquals(fotosUsuario, resp);

            verify(usuarioRepo).findUsuarioForId(userId);
        }

        @Test
        void getFotosUsuario_UsuarioNoEncontrado() {
            Long userId = 1L;

            // Simular que el usuario no existe
            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.empty());

            // Llamar al método
            List<String> fotos = usuarioService.getFotosUsuario(userId);

            // Verificar que devuelve null
            assertNull(fotos);

            // Verificar que el repositorio fue llamado
            verify(usuarioRepo).findUsuarioForId(userId);
        }
    }

    // ========================
    // Tests getRollUsuario()
    // ========================
    @Nested
    class GetRollUsuarioTests {
        @Test
        void getRollUsuario_SuccessfulRetrieval() {
            Long userId = 1L; // Usuario 1
            RoleEnum roll = RoleEnum.USER;
            Usuario usuario = new Usuario();
            usuario.setRollUsuario(roll);
            usuario.setIdUsuario(userId);

            when(usuarioRepo.findRollUsuarioForId(userId)).thenReturn(Optional.of(roll));

            RoleEnum resp = usuarioService.getRollUsuario(userId);

            assertNotNull(resp);
            assertEquals(roll, resp);

            verify(usuarioRepo).findRollUsuarioForId(userId);
        }

        @Test
        void getRollUsuario_Error() {
            Long userId = 1L;

            // Simular que el usuario no existe
            when(usuarioRepo.findRollUsuarioForId(userId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class,
                    () -> usuarioService.getRollUsuario(userId));

            assertEquals("Error en obtener el rol del usuario con ID " + userId, thrown.getMessage());

            verify(usuarioRepo).findRollUsuarioForId(userId);
        }
    }

    // =========================
    // Tests getPlanUsuario()
    // =========================
    @Nested
    class GetPlanUsuarioTests {
        @Test
        void getPlanUsuario_SuccessfulRetrieval() {
            Long userId = 1L;
            Plan plan = new Plan();
            plan.setIdPlan(1L);
            plan.setNombrePlan("Plan 1");
            plan.setCursosPlan(List.of(new Curso()));

            when(usuarioRepo.findPlanUsuarioForId(userId)).thenReturn(Optional.of(plan));

            Plan resp = usuarioService.getPlanUsuario(userId);

            assertNotNull(resp);
            assertEquals(plan.getIdPlan(), resp.getIdPlan());
            assertEquals(plan.getNombrePlan(), resp.getNombrePlan());
            assertEquals(plan.getCursosPlan(), resp.getCursosPlan());
        }

        @Test
        void getPlanUsuario_Error() {
            Long userId = 1L;

            // Simula que no se encuentra plan (ni usuario)
            when(usuarioRepo.findPlanUsuarioForId(userId)).thenReturn(Optional.empty());

            // Espera una EntityNotFoundException
            EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class,
                    () -> usuarioService.getPlanUsuario(userId));

            assertEquals("Error en obtener el plan del usuario con ID " + userId, thrown.getMessage());

            verify(usuarioRepo).findPlanUsuarioForId(userId);
        }
    }

    // =========================
    // Tests getCursosUsuario()
    // =========================
    @Nested
    class GetCursosUsuarioTests {
        @Test
        void getCursosUsuario_SuccessfulRetrieval() {
            Long userId = 1L;
            List<Curso> cursosUsuario = List.of(new Curso());
            Usuario usuario = new Usuario();
            usuario.setCursosUsuario(cursosUsuario);
            usuario.setIdUsuario(userId);

            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.of(usuario));

            List<Curso> resp = usuarioService.getCursosUsuario(userId);

            assertNotNull(resp);
            assertEquals(cursosUsuario, resp);

            verify(usuarioRepo).findUsuarioForId(userId);
        }

        @Test
        void getCursosUsuario_Error() {
            Long userId = 1L;

            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.empty());

            List<Curso> resp = usuarioService.getCursosUsuario(userId);

            assertNull(resp);

            verify(usuarioRepo).findUsuarioForId(userId);
        }
    }

    // =========================
    // Tests updateUsuario()
    // =========================
    @Nested
    class UpdateUsuarioTests {
        @Test
        void updateUsuario_SuccessfulUpdate() throws InternalServerException {
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setFotoUsuario(new ArrayList<>(List.of("foto1.png")));

            Usuario usuarioOld = new Usuario();
            usuarioOld.setIdUsuario(1L);
            usuarioOld.setFotoUsuario(new ArrayList<>(List.of("foto1.png")));

            UsuarioService spyService = spy(usuarioService);

            doReturn(usuarioOld).when(spyService).getUsuario(1L);
            when(usuarioRepo.save(any(Usuario.class))).thenReturn(usuario);

            Usuario resp = spyService.updateUsuario(usuario);

            assertNotNull(resp);
            assertEquals(usuario, resp);

            verify(usuarioRepo).save(usuario);
            verify(spyService).getUsuario(1L);
        }

        @Test
        void updateUsuario_WhenDeletePhotoFails_ThrowsInternalServerException(@TempDir Path tempDir) throws Exception {
            // Arrange
            UsuarioService spyService = spy(usuarioService);

            // Usuario viejo con una foto
            Usuario usuarioOld = new Usuario();
            usuarioOld.setIdUsuario(1L);
            usuarioOld.setFotoUsuario(new ArrayList<>(List.of("foto/fotoVieja.png")));

            // Usuario nuevo sin esa foto
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setFotoUsuario(new ArrayList<>());

            // Creamos un directorio no vacío para forzar IOException
            Path fotoDir = tempDir.resolve("fotoVieja.png");
            Files.createDirectory(fotoDir);
            Files.createFile(fotoDir.resolve("dummy.txt")); // 👈 provoca DirectoryNotEmptyException

            // Inyectamos uploadDir
            Field uploadDirField = UsuarioService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(spyService, tempDir.toString());

            // Simulamos que getUsuario devuelve el usuario viejo
            doReturn(usuarioOld).when(spyService).getUsuario(1L);

            // Act + Assert
            InternalServerException ex = assertThrows(
                    InternalServerException.class,
                    () -> spyService.updateUsuario(usuario));

            assertTrue(ex.getMessage().contains("Error al eliminar la foto"));
        }

        @Test
        void updateUsuario_WhenPhotoExists_IsDeleted(@TempDir Path tempDir) throws Exception {
            // Usuario viejo con foto
            Usuario usuarioOld = new Usuario();
            usuarioOld.setIdUsuario(1L);
            usuarioOld.setFotoUsuario(List.of("fotoVieja.png"));

            // Usuario nuevo sin esa foto
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setFotoUsuario(new ArrayList<>());

            // Archivo temporal
            Path fotoPath = tempDir.resolve("fotoVieja.png");
            Files.createFile(fotoPath);

            // Configurar uploadDir
            Field uploadDirField = UsuarioService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(usuarioService, tempDir.toString());

            // Configurar mocks
            when(usuarioRepo.findUsuarioForId(1L)).thenReturn(Optional.of(usuarioOld));
            when(usuarioRepo.save(usuario)).thenReturn(usuario);

            // Act
            Usuario resp = usuarioService.updateUsuario(usuario);

            // Assert
            assertNotNull(resp);
            assertEquals(usuario, resp);
            assertFalse(Files.exists(fotoPath));
        }

        @Test
        void updateUsuario_WhenPhotoDoesNotExist_DoesNotThrow(@TempDir Path tempDir) throws Exception {
            // Usuario viejo con una foto que no existe físicamente
            Usuario usuarioOld = new Usuario();
            usuarioOld.setIdUsuario(1L);
            usuarioOld.setFotoUsuario(List.of("fotoInexistente.png"));

            // Usuario nuevo sin esa foto
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setFotoUsuario(new ArrayList<>());

            // Configurar uploadDir
            Field uploadDirField = UsuarioService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(usuarioService, tempDir.toString());

            // Configurar mocks
            when(usuarioRepo.findUsuarioForId(1L)).thenReturn(Optional.of(usuarioOld));
            when(usuarioRepo.save(usuario)).thenReturn(usuario);

            // Act
            Usuario resp = usuarioService.updateUsuario(usuario);

            // Assert
            assertNotNull(resp);
            assertEquals(usuario, resp);

            // Comprobar que el archivo realmente no existe
            Path expectedPath = tempDir.resolve("fotoInexistente.png");
            assertFalse(Files.exists(expectedPath));

            // Verificar interacción con el repo
            verify(usuarioRepo).save(usuario);
            verify(usuarioRepo).findUsuarioForId(1L);
        }
    }

    // =========================
    // Tests changePlanUsuario()
    // =========================
    @Nested
    class ChangePlanUsuarioTests {
        @Test
        void changePlanUsuario_SuccessfulUpdate() {
            // Arrange
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);

            Plan plan = new Plan();
            plan.setIdPlan(1L); // Long
            plan.setNombrePlan("Plan 1");
            plan.setCursosPlan(List.of(new Curso()));

            usuario.setPlanUsuario(plan);

            // Stub correcto: Optional<Integer>
            when(usuarioRepo.changePlanUsuarioForId(usuario.getIdUsuario(), usuario.getPlanUsuario()))
                    .thenReturn(Optional.of(plan.getIdPlan().intValue()));

            // Act
            Integer resp = usuarioService.changePlanUsuario(usuario);

            // Assert
            assertNotNull(resp);
            assertEquals(1, resp);

            verify(usuarioRepo).changePlanUsuarioForId(usuario.getIdUsuario(), usuario.getPlanUsuario());
        }

        @Test
        void changePlanUsuario_WhenRepoReturnsEmpty_ThrowsEntityNotFoundException() {
            // Arrange
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);

            Plan plan = new Plan();
            plan.setIdPlan(1L);
            usuario.setPlanUsuario(plan);

            // Simulamos que el repo devuelve Optional.empty()
            when(usuarioRepo.changePlanUsuarioForId(usuario.getIdUsuario(), usuario.getPlanUsuario()))
                    .thenReturn(Optional.empty());

            // Act + Assert
            EntityNotFoundException ex = assertThrows(
                    EntityNotFoundException.class,
                    () -> usuarioService.changePlanUsuario(usuario));

            assertTrue(ex.getMessage().contains("Error en cambiar el plan del usuario"));

            verify(usuarioRepo).changePlanUsuarioForId(usuario.getIdUsuario(), usuario.getPlanUsuario());
        }
    }

    // =========================
    // Tests changeCursosUsuario()
    // =========================
    @Nested
    class ChangeCursosUsuarioTests {
        @Test
        void changeCursosUsuario_SuccessfulUpdate_MockWebClientResponses() throws Exception {
            // Arrange
            Usuario oldUsuario = new Usuario();
            oldUsuario.setIdUsuario(1L);

            Curso curso1 = new Curso();
            Curso curso2 = new Curso();
            List<Long> idsCursos = List.of(101L, 102L);

            CursosUsuario cursosUsuario = new CursosUsuario(1L, idsCursos);

            // Mock repos
            when(usuarioRepo.findUsuarioForId(1L)).thenReturn(Optional.of(oldUsuario));
            when(cursoRepo.findAllById(idsCursos)).thenReturn(List.of(curso1, curso2));
            when(usuarioRepo.changeUsuarioForId(eq(1L), any(Usuario.class))).thenReturn(Optional.of(1));

            // Spy del servicio
            UsuarioService spyService = spy(usuarioService);

            // Mock WebClient para createUsuarioStream
            WebClient mockWebClientStream = mock(WebClient.class, RETURNS_DEEP_STUBS);
            when(webClientConfig.createSecureWebClient(backStreamURL)).thenReturn(mockWebClientStream);
            when(mockWebClientStream.put()
                    .uri("/nuevoUsuario")
                    .body(any(Mono.class), eq(Usuario.class))
                    .retrieve()
                    .bodyToMono(String.class))
                    .thenAnswer(invocation -> {
                        // Ejecutar el Mono y cubrir el subscribe
                        Mono<String> mono = Mono.just("Nuevo Usuario Insertado con Exito!!!");
                        return mono.doOnNext(res -> {
                            if (res == null || !res.equals("Nuevo Usuario Insertado con Exito!!!")) {
                                spyService.logger.error("Error inesperado al crear el usuario en el stream: {}",
                                        res);
                            }
                        }).block();
                    });

            // Mock WebClient para createUsuarioChat
            WebClient mockWebClientChat = mock(WebClient.class, RETURNS_DEEP_STUBS);
            when(webClientConfig.createSecureWebClient(backChatURL)).thenReturn(mockWebClientChat);
            when(mockWebClientChat.post()
                    .uri("/crea_usuario_chat")
                    .body(any(Mono.class), eq(Usuario.class))
                    .retrieve()
                    .bodyToMono(String.class))
                    .thenAnswer(invocation -> {
                        Mono<String> mono = Mono.just("Usuario chat creado con exito!!!");
                        return mono.doOnNext(res -> {
                            if (res == null || !res.equals("Usuario chat creado con exito!!!")) {
                                spyService.logger.error("Error en crear el usuario en el chat: {}", res);
                            }
                        }).block();
                    });

            // Act
            Integer result = spyService.changeCursosUsuario(cursosUsuario);

            // Assert
            assertEquals(1, result);
            assertEquals(List.of(curso1, curso2), oldUsuario.getCursosUsuario());

            // Verificaciones
            verify(usuarioRepo).findUsuarioForId(1L);
            verify(cursoRepo).findAllById(idsCursos);
            verify(usuarioRepo).changeUsuarioForId(1L, oldUsuario);
            verify(spyService).createUsuarioChat(oldUsuario);
            verify(spyService).createUsuarioStream(oldUsuario);
        }

        @Test
        void changeCursosUsuario_WhenUserNotFound_ThrowsIllegalArgumentException() {
            // Arrange
            CursosUsuario cursosUsuario = new CursosUsuario(1L, List.of(101L, 102L));
            when(usuarioRepo.findUsuarioForId(1L)).thenReturn(Optional.empty());

            // Act + Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> usuarioService.changeCursosUsuario(cursosUsuario));

            assertEquals("El usuario no existe", ex.getMessage());

            verify(usuarioRepo).findUsuarioForId(1L);
            verifyNoInteractions(cursoRepo);
        }

        @Test
        void changeCursosUsuario_errorCreateUsuarioStream_Response404_NoThrowsException()
                throws InternalComunicationException, RepositoryException, SSLException, URISyntaxException {

            Long id = 1L;

            Usuario oldUsuario = new Usuario();
            oldUsuario.setIdUsuario(id);

            Curso curso1 = new Curso();
            Curso curso2 = new Curso();

            List<Long> idsCursos = List.of(101L, 102L);
            CursosUsuario cursosUsuario = new CursosUsuario(id, idsCursos);

            // Mock repositorios
            when(usuarioRepo.findUsuarioForId(id)).thenReturn(Optional.of(oldUsuario));
            when(cursoRepo.findAllById(idsCursos)).thenReturn(List.of(curso1, curso2));
            when(usuarioRepo.changeUsuarioForId(eq(id), any(Usuario.class))).thenReturn(Optional.of(1));

            // Mock del WebClient del microservicio de stream con deep stubs
            WebClient mockWebClientStream = mock(WebClient.class, RETURNS_DEEP_STUBS);
            when(webClientConfig.createSecureWebClient(backStreamURL)).thenReturn(mockWebClientStream);

            // Simular 404 directamente en bodyToMono encadenando todos los métodos
            when(mockWebClientStream.put()
                    .uri("/nuevoUsuario")
                    .body(any(Mono.class), eq(Usuario.class))
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(String.class))
                    .thenReturn(Mono.error(new WebClientResponseException(
                            404,
                            "Not Found",
                            HttpHeaders.EMPTY,
                            null,
                            null)));

            // Ejecutar → no debe lanzar excepción
            Integer result = usuarioService.changeCursosUsuario(cursosUsuario);

            // Verificaciones básicas
            assertEquals(1, result);
            verify(usuarioRepo).findUsuarioForId(id);
            verify(cursoRepo).findAllById(idsCursos);
            verify(usuarioRepo).changeUsuarioForId(eq(id), any(Usuario.class));
        }

        @Test
        void changeCursosUsuario_errorCreateUsuarioChat_NoThrowsException()
                throws RepositoryException, InternalComunicationException, SSLException, URISyntaxException {

            Long id = 1L;

            Usuario oldUsuario = new Usuario();
            oldUsuario.setIdUsuario(id);

            Curso curso1 = new Curso();
            Curso curso2 = new Curso();

            List<Long> idsCursos = List.of(101L, 102L);
            CursosUsuario cursosUsuario = new CursosUsuario(id, idsCursos);

            // Mock repos
            when(usuarioRepo.findUsuarioForId(id)).thenReturn(Optional.of(oldUsuario));
            when(cursoRepo.findAllById(idsCursos)).thenReturn(List.of(curso1, curso2));
            when(usuarioRepo.changeUsuarioForId(eq(id), any(Usuario.class))).thenReturn(Optional.of(1));

            // Spy sobre el service real
            UsuarioService spyService = spy(usuarioService);

            // Simular WebClient para createUsuarioStream
            WebClient mockWebClientStream = mock(WebClient.class, RETURNS_DEEP_STUBS);
            when(webClientConfig.createSecureWebClient(backStreamURL)).thenReturn(mockWebClientStream);
            when(mockWebClientStream.put()
                    .uri("/nuevoUsuario")
                    .body(any(Mono.class), eq(Usuario.class))
                    .retrieve()
                    .bodyToMono(String.class))
                    .thenReturn(Mono.just("Nuevo Usuario Insertado con Exito!!!"));

            // Mock del WebClient del microservicio de stream
            WebClient mockWebClientStream2 = mock(WebClient.class, RETURNS_DEEP_STUBS);

            when(webClientConfig.createSecureWebClient(backStreamURL))
                    .thenReturn(mockWebClientStream2);

            // Simular 404
            // Simular 404 directamente en bodyToMono encadenando todos los métodos

            when(mockWebClientStream2.put()
                    .uri("/crea_usuario_chat")
                    .body(any(Mono.class), eq(Usuario.class))
                    .retrieve()
                    .onStatus(any(), any())

                    .bodyToMono(String.class))
                    .thenReturn(Mono.error(new WebClientResponseException(
                            404,
                            "Not Found",
                            HttpHeaders.EMPTY,
                            null,
                            null)));

            // Ejecutar → NO debe lanzar excepción
            Integer result = spyService.changeCursosUsuario(cursosUsuario);

            assertEquals(1, result);

            // Verificaciones
            verify(usuarioRepo).findUsuarioForId(id);
            verify(cursoRepo).findAllById(idsCursos);
            verify(spyService).createUsuarioStream(any(Usuario.class)); // se llamó
            verify(spyService).createUsuarioChat(any(Usuario.class)); // se llamó y falló
            verify(usuarioRepo).changeUsuarioForId(eq(id), any(Usuario.class));
        }

        @Test
        void changeCursosUsuario_RepositoryError_ThrowsRepositoryException() {
            // Arrange
            Usuario oldUsuario = new Usuario();
            oldUsuario.setIdUsuario(1L);

            Curso curso1 = new Curso();
            Curso curso2 = new Curso();
            List<Long> idsCursos = List.of(101L, 102L);

            CursosUsuario cursosUsuario = new CursosUsuario(1L, idsCursos);
            when(usuarioRepo.findUsuarioForId(1L)).thenReturn(Optional.of(oldUsuario));
            when(cursoRepo.findAllById(idsCursos)).thenReturn(List.of(curso1, curso2));

            when(usuarioRepo.changeUsuarioForId(eq(1L), any(Usuario.class))).thenReturn(Optional.empty());

            RepositoryException ex = assertThrows(
                    RepositoryException.class,
                    () -> usuarioService.changeCursosUsuario(cursosUsuario));

            assertTrue(ex.getMessage().contains("Error en cambiar los cursos del usuario"));

            verify(usuarioRepo).findUsuarioForId(1L);
            verify(cursoRepo).findAllById(idsCursos);
            verify(usuarioRepo).changeUsuarioForId(1L, oldUsuario);
        }
    }

    // ==========================
    // Tests deleteUsuario()
    // ==========================
    @Nested
    class DeleteUsuarioTests {
        @Test
        void deleteUsuario_SuccessfulDeletion() throws RepositoryException, InternalComunicationException {
            UsuarioService spyService = spy(usuarioService);
            Long userId = 1L;
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(userId);
            String aspect = "Usuario eliminado con éxito!!!";
            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.of(usuario));

            String resp = spyService.deleteUsuario(userId);

            assertNotNull(resp);
            assertEquals(aspect, resp);

            verify(usuarioRepo).findUsuarioForId(userId);
            verify(loginRepo).deleteById(userId);

            verify(loginRepo).deleteById(userId);
            verify(usuarioRepo).deleteById(userId);
            verify(initAppService).refreshSSR();
        }

        @Test
        void deleteUsuario_UsuarioNotFound_ThrowsEntityNotFoundException() {
            Long userId = 1L;
            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(
                    EntityNotFoundException.class,
                    () -> usuarioService.deleteUsuario(userId));

            assertTrue(ex.getMessage().contains("Error en obtener el usuario con ID " + userId));

            verify(usuarioRepo).findUsuarioForId(userId);
            verifyNoInteractions(loginRepo);
        }

        @Test
        void deleteUsuario_ErrorDeletingLogin_ThrowsRepositoryException() {
            Long userId = 1L;
            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.of(new Usuario()));

            // 👇 Para métodos void
            doThrow(new IllegalArgumentException("Error al eliminar el login"))
                    .when(loginRepo).deleteById(userId);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> usuarioService.deleteUsuario(userId));

            assertTrue(ex.getMessage().contains("Error en eliminar el usuario con ID"));

            verify(usuarioRepo).findUsuarioForId(userId);
            verify(loginRepo).deleteById(userId);
            verify(usuarioRepo, never()).deleteById(userId); // porque falló antes
        }

        @Test
        void deleteUsuario_ErrorDeletingUser_ThrowsRepositoryException() {
            Long userId = 1L;
            when(usuarioRepo.findUsuarioForId(userId)).thenReturn(Optional.of(new Usuario()));

            // 👇 Para métodos void
            doThrow(new IllegalArgumentException("Error al eliminar el usuario"))
                    .when(usuarioRepo).deleteById(userId);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> usuarioService.deleteUsuario(userId));

            assertTrue(ex.getMessage().contains("Error en eliminar el usuario con ID"));

            verify(usuarioRepo).findUsuarioForId(userId);
            verify(loginRepo).deleteById(userId);
        }

        @Test
        void deleteUsuario_ErrorUpdateSSR_NoThrowsException()
                throws InternalComunicationException, RepositoryException {
            Long id = 1L;
            when(usuarioRepo.findUsuarioForId(id)).thenReturn(Optional.of(new Usuario()));

            // refreshSSR lanza excepción controlada
            doThrow(new InternalComunicationException("fallo SSR"))
                    .when(initAppService)
                    .refreshSSR();

            // Spy para interceptar métodos protected
            UsuarioService spy = Mockito.spy(usuarioService);

            doNothing().when(spy).deleteUsuarioChat(id);
            doNothing().when(spy).deleteUsuarioStream(id);

            // Ejecutar → NO debe lanzar excepción
            String resp = spy.deleteUsuario(id);

            assertEquals("Usuario eliminado con éxito!!!", resp);

            verify(loginRepo).deleteById(id);
            verify(usuarioRepo).deleteById(id);
            verify(initAppService).refreshSSR();

            // El flujo continúa
            verify(spy).deleteUsuarioChat(id);
            verify(spy).deleteUsuarioStream(id);
        }
    }

    // ==========================
    // Tests getProfes()
    // ==========================
    @Nested
    class GetProfesTests {
        @Test
        void getProfes_SuccessfulRetrieval() {
            List<Usuario> profesores = List.of(new Usuario(), new Usuario());
            when(usuarioRepo.findProfes()).thenReturn(profesores);

            List<Usuario> resp = usuarioService.getProfes();

            assertNotNull(resp);
            assertEquals(profesores, resp);

            verify(usuarioRepo).findProfes();
        }
    }

    // ==========================
    // Tests sendConfirmationEmail()
    // ==========================
    @Nested
    class SendConfirmationEmailTests {
        @Test
        void sendConfirmationEmail_SuccessfulSend() throws Exception {
            // Arrange
            NewUsuario newUsuario = new NewUsuario("pepito", "pepito@example.com", "pwd", null, null, null, null);
            when(jwtUtil.generateRegistrationToken(any(NewUsuario.class))).thenReturn("reg-token");
            when(templateEngine.process(eq("mail-registro"), any(Context.class))).thenReturn("<html>hola</html>");

            // MimeMessage real (no problemas al construir MimeMessageHelper)
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            boolean result = usuarioService.sendConfirmationEmail(newUsuario);

            // Assert
            assertTrue(result);
            verify(jwtUtil).generateRegistrationToken(any(NewUsuario.class));
            verify(templateEngine).process(eq("mail-registro"), any(Context.class));
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(eq(mimeMessage));
        }

        @Test
        void sendConfirmationEmail_WhenMailAuthenticationException_ThrowsInternalServerException() throws Exception {
            // Arrange
            NewUsuario newUsuario = new NewUsuario("pepito", "pepito@example.com", "pwd", null, null, null, null);
            when(jwtUtil.generateRegistrationToken(any(NewUsuario.class))).thenReturn("reg-token");
            when(templateEngine.process(eq("mail-registro"), any(Context.class))).thenReturn("<html>hola</html>");

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            doThrow(new MailAuthenticationException("auth failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            // Act / Assert
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> usuarioService.sendConfirmationEmail(newUsuario));
            assertTrue(ex.getMessage().contains("Error de autenticación al enviar el correo"));
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        void sendConfirmationEmail_WhenMailSendException_ThrowsInternalServerException() throws Exception {
            // Arrange
            NewUsuario newUsuario = new NewUsuario("pepito", "pepito@example.com", "pwd", null, null, null, null);
            when(jwtUtil.generateRegistrationToken(any(NewUsuario.class))).thenReturn("reg-token");
            when(templateEngine.process(eq("mail-registro"), any(Context.class))).thenReturn("<html>hola</html>");

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            doThrow(new MailSendException("send failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            // Act / Assert
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> usuarioService.sendConfirmationEmail(newUsuario));
            assertTrue(ex.getMessage().contains("Error al enviar el correo"));
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        void sendConfirmationEmail_WhenGenericMailException_ThrowsInternalServerException() throws Exception {
            // Arrange
            NewUsuario newUsuario = new NewUsuario("pepito", "pepito@example.com", "pwd", null, null, null, null);
            when(jwtUtil.generateRegistrationToken(any(NewUsuario.class))).thenReturn("reg-token");
            when(templateEngine.process(eq("mail-registro"), any(Context.class))).thenReturn("<html>hola</html>");

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // MailParseException is a concrete subclass of MailException (not
            // MailSend/MailAuth)
            doThrow(new MailParseException("parse error"))
                    .when(mailSender).send(any(MimeMessage.class));

            // Act / Assert
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> usuarioService.sendConfirmationEmail(newUsuario));
            assertTrue(ex.getMessage().contains("Error general al enviar el correo"));
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        void sendConfirmationEmail_WhenCreateMimeMessageFails_ThrowsInternalServerException()
                throws InternalServerException {
            // Arrange
            NewUsuario newUsuario = new NewUsuario("pepe", "pepe@example.com", "pwd", null, null, null, null);
            when(jwtUtil.generateRegistrationToken(any())).thenReturn("tok");
            when(templateEngine.process(eq("mail-registro"), any(Context.class)))
                    .thenReturn("<html>ok</html>");

            // 👉 Simulamos fallo al crear el MimeMessage con una excepción real de Spring
            when(mailSender.createMimeMessage()).thenThrow(new MailSendException("broken MIME"));

            // Act + Assert
            InternalServerException ex = assertThrows(
                    InternalServerException.class,
                    () -> usuarioService.sendConfirmationEmail(newUsuario));

            assertTrue(ex.getMessage().contains("Error al enviar el correo"));
        }

        @Test
        void sendConfirmationEmail_WhenUnexpectedException_ThrowsInternalServerException() throws Exception {
            // Arrange
            NewUsuario newUsuario = new NewUsuario("pepito", "pepito@example.com", "pwd", null, null, null, null);
            when(jwtUtil.generateRegistrationToken(any(NewUsuario.class))).thenReturn("reg-token");
            when(templateEngine.process(eq("mail-registro"), any(Context.class))).thenReturn("<html>hola</html>");

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Forzar una excepción no relacionada con MailException (ej. RuntimeException)
            // en send
            doThrow(new RuntimeException("boom"))
                    .when(mailSender).send(any(MimeMessage.class));

            // Act / Assert
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> usuarioService.sendConfirmationEmail(newUsuario));
            assertTrue(ex.getMessage().contains("Error inesperado al enviar el correo"));
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    // ==========================
    // Tests getAllUsuarios()
    // ==========================
    @Nested
    class GetAllUsuariosTests {
        @Test
        void getAllUsuarios_SuccessfulRetrieval() throws RepositoryException {
            List<Usuario> usuarios = List.of(new Usuario(), new Usuario());
            when(usuarioRepo.findAll()).thenReturn(usuarios);

            List<Usuario> resp = usuarioService.getAllUsuarios();

            assertNotNull(resp);
            assertEquals(usuarios, resp);

            verify(usuarioRepo).findAll();
        }
    }

    /*
     * // ==========================
     * // Tests deleteUsuarioStream()
     * // ==========================
     * 
     * @Test
     * void deleteUsuarioStream_SuccessfulDeletion() throws SSLException,
     * URISyntaxException {
     * Long userId = 1L;
     * when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
     * when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.
     * just("Usuario stream borrado con exito!!!"));
     * 
     * usuarioService.deleteUsuarioStream(userId);
     * 
     * verify(webClientConfig).createSecureWebClient(anyString());
     * verify(webClient).delete();
     * verify(requestHeadersUriSpec).uri("/deleteUsuarioStream/" + userId);
     * }
     * 
     * // ==========================
     * // Tests deleteUsuarioChat()
     * // ==========================
     * 
     * @Test
     * void deleteUsuarioChat_SuccessfulDeletion() throws SSLException,
     * URISyntaxException {
     * Long userId = 1L;
     * when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
     * when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.
     * just("Usuario chat borrado con exito!!!"));
     * 
     * usuarioService.deleteUsuarioChat(userId);
     * 
     * verify(webClientConfig).createSecureWebClient(anyString());
     * verify(webClient).delete();
     * verify(requestHeadersUriSpec).uri("/delete_usuario_chat/" + userId);
     * }
     * 
     */

    @Mock
    private WebClientConfig webClientConfig;

    // Usamos RETURNS_DEEP_STUBS para evitar tener que mockear cada etapa
    // manualmente
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private LoginRepository loginRepo;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private InitAppService initAppService;
    @Mock
    private SpringTemplateEngine templateEngine;

    @TempDir
    Path tempDir;

    private UsuarioService usuarioService;
    private final String backChatURL = "http://mocked-chat-url";
    private final String backStreamURL = "http://mocked-stream-url";
    private final String frontURL = "http://mocked-front-url";

    @BeforeEach
    void setUp() throws SSLException, URISyntaxException {
        // Con RETURNS_DEEP_STUBS no necesitamos mockear
        // RequestHeadersUriSpec/RequestHeadersSpec/ResponseSpec manualmente
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);

        // Crear servicio con dependencias mock
        usuarioService = new UsuarioService(
                passwordEncoder,
                usuarioRepo,
                cursoRepo,
                loginRepo,
                jwtUtil,
                webClientConfig,
                mailSender,
                initAppService,
                templateEngine,
                backChatURL,
                backStreamURL,
                frontURL,
                tempDir.toString());
    }

}
