package com.sovereingschool.back_base.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.DTOs.InitApp;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class InitAppServiceTest {

    // ==========================
    // Tests getProfesores()
    // ==========================
    @Nested
    class GetProfesoresTests {

        @Test
        void getProfesores_SuccessfulRetrieval() {
            // Arrange
            List<Usuario> expectedProfesores = Arrays.asList(
                    Usuario.builder()
                            .idUsuario(1L)
                            .nombreUsuario("Prof1")
                            .fotoUsuario(Arrays.asList("foto1"))
                            .presentacion("presentacion1")
                            .rollUsuario(RoleEnum.PROF)
                            .build(),
                    Usuario.builder()
                            .idUsuario(2L)
                            .nombreUsuario("Prof2")
                            .fotoUsuario(Arrays.asList("foto2"))
                            .presentacion("presentacion2")
                            .rollUsuario(RoleEnum.PROF)
                            .build());
            when(usuarioRepo.getInit()).thenReturn(expectedProfesores);

            // Act
            List<Usuario> result = initAppService.getProfesores();

            // Assert
            assertNotNull(result);
            assertEquals(expectedProfesores, result);
            verify(usuarioRepo).getInit();
        }

        @Test
        void getProfesores_EmptyList() {
            // Arrange
            when(usuarioRepo.getInit()).thenReturn(Arrays.asList());

            // Act
            List<Usuario> result = initAppService.getProfesores();

            // Assert
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(usuarioRepo).getInit();
        }
    }

    // ==========================
    // Tests getInit()
    // ==========================
    @Nested
    class GetInitTests {

        @Test
        void getInit_SuccessfulRetrieval() {
            // Arrange
            List<Usuario> profes = Arrays.asList(
                    Usuario.builder()
                            .idUsuario(1L)
                            .nombreUsuario("Prof1")
                            .fotoUsuario(Arrays.asList("foto1"))
                            .presentacion("presentacion1")
                            .rollUsuario(RoleEnum.PROF)
                            .build(),
                    Usuario.builder()
                            .idUsuario(2L)
                            .nombreUsuario("Prof2")
                            .fotoUsuario(Arrays.asList("foto2"))
                            .presentacion("presentacion2")
                            .rollUsuario(RoleEnum.PROF)
                            .build());
            List<Curso> cursos = Arrays.asList(
                    createCurso(1L, "Curso1", Arrays.asList(profes.get(0)), "desc corta", "imagen1"),
                    createCurso(2L, "Curso2", Arrays.asList(profes.get(1)), "desc corta2", "imagen2"));

            when(usuarioRepo.getInit()).thenReturn(profes);
            when(cursoRepo.getAllCursos()).thenReturn(cursos);
            when(usuarioRepo.count()).thenReturn(10L);
            when(cursoRepo.count()).thenReturn(5L);
            when(claseRepo.count()).thenReturn(20L);

            // Act
            InitApp result = initAppService.getInit();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.profesInit().size());
            assertEquals(2, result.cursosInit().size());
            assertEquals(2, result.estadistica().profesores());
            assertEquals(8, result.estadistica().alumnos());
            assertEquals(5, result.estadistica().cursos());
            assertEquals(20, result.estadistica().clases());

            verify(usuarioRepo).getInit();
            verify(cursoRepo).getAllCursos();
            verify(usuarioRepo).count();
            verify(cursoRepo).count();
            verify(claseRepo).count();
        }

        @Test
        void getInit_EmptyLists() {
            // Arrange
            when(usuarioRepo.getInit()).thenReturn(Arrays.asList());
            when(cursoRepo.getAllCursos()).thenReturn(Arrays.asList());
            when(usuarioRepo.count()).thenReturn(0L);
            when(cursoRepo.count()).thenReturn(0L);
            when(claseRepo.count()).thenReturn(0L);

            // Act
            InitApp result = initAppService.getInit();

            // Assert
            assertNotNull(result);
            assertEquals(0, result.profesInit().size());
            assertEquals(0, result.cursosInit().size());
            assertEquals(0, result.estadistica().profesores());
            assertEquals(0, result.estadistica().alumnos());
            assertEquals(0, result.estadistica().cursos());
            assertEquals(0, result.estadistica().clases());
        }
    }

    // ==========================
    // Tests getInitToken()
    // ==========================
    @Nested
    class GetInitTokenTests {

        @Test
        void getInitToken_SuccessfulGeneration() {
            // Arrange
            String expectedToken = "mocked-jwt-token";
            when(jwtUtil.generateInitToken()).thenReturn(expectedToken);

            // Act
            String result = initAppService.getInitToken();

            // Assert
            assertNotNull(result);
            assertEquals(expectedToken, result);
            verify(jwtUtil).generateInitToken();
        }
    }

    // ==========================
    // Tests refreshSSR()
    // ==========================
    @Nested
    class RefreshSSRTests {

        @Test
        void refreshSSR_SuccessfulRefresh() throws InternalComunicationException {
            // Arrange
            List<Usuario> profes = Arrays.asList(Usuario.builder()
                    .idUsuario(1L)
                    .nombreUsuario("Prof1")
                    .fotoUsuario(Arrays.asList("foto1"))
                    .presentacion("presentacion1")
                    .rollUsuario(RoleEnum.PROF)
                    .build());
            List<Curso> cursos = Arrays.asList(createCurso(1L, "Curso1", Arrays.asList(profes.get(0)), "desc", "img"));
            when(usuarioRepo.getInit()).thenReturn(profes);
            when(cursoRepo.getAllCursos()).thenReturn(cursos);
            when(usuarioRepo.count()).thenReturn(1L);
            when(cursoRepo.count()).thenReturn(1L);
            when(claseRepo.count()).thenReturn(1L);

            when(webClient.post()
                    .uri("/refresh-cache")
                    .body(any(Mono.class), any(Class.class))
                    .retrieve()
                    .bodyToMono(String.class)).thenReturn(Mono.just("Cache global actualizado con éxito"));

            // Act & Assert
            // Como es asíncrono, difícil testear directamente, pero verificar que no lance
            // excepción
            initAppService.refreshSSR();
        }

        @Test
        void refreshSSR_ErrorInCommunication() {
            // Arrange
            List<Usuario> profes = Arrays.asList(Usuario.builder()
                    .idUsuario(1L)
                    .nombreUsuario("Prof1")
                    .fotoUsuario(Arrays.asList("foto1"))
                    .presentacion("presentacion1")
                    .rollUsuario(RoleEnum.PROF)
                    .build());
            List<Curso> cursos = Arrays.asList(createCurso(1L, "Curso1", Arrays.asList(profes.get(0)), "desc", "img"));
            when(usuarioRepo.getInit()).thenReturn(profes);
            when(cursoRepo.getAllCursos()).thenReturn(cursos);
            when(usuarioRepo.count()).thenReturn(1L);
            when(cursoRepo.count()).thenReturn(1L);
            when(claseRepo.count()).thenReturn(1L);

            // Mock para lanzar error
            when(webClient.post().uri("/refresh-cache").body(any(Mono.class), any(Class.class)).retrieve())
                    .thenThrow(new RuntimeException("Connection error"));

            // Act & Assert
            assertThrows(InternalComunicationException.class, () -> initAppService.refreshSSR());
        }
    }

    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private WebClientConfig webClientConfig;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    private InitAppService initAppService;
    private final String frontDocker = "http://mocked-front-url";

    @BeforeEach
    void setUp() throws SSLException, URISyntaxException {
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);
        initAppService = new InitAppService(
                frontDocker,
                cursoRepo,
                claseRepo,
                usuarioRepo,
                jwtUtil,
                webClientConfig);
    }

    private Curso createCurso(Long id, String nombre, List<Usuario> profesores, String descCorta, String imagen) {
        Curso curso = new Curso();
        curso.setIdCurso(id);
        curso.setNombreCurso(nombre);
        curso.setProfesoresCurso(profesores);
        curso.setDescripcionCorta(descCorta);
        curso.setImagenCurso(imagen);
        return curso;
    }
}
