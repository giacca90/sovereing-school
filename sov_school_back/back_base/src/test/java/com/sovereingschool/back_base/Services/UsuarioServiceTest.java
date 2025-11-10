package com.sovereingschool.back_base.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private WebClientConfig webClientConfig;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
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

    private UsuarioService usuarioService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SSLException, URISyntaxException {
        // Lenient mocks para WebClient (evita UnnecessaryStubbingException)
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);
        lenient().when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

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
                "http://mocked-chat-url",
                "http://mocked-stream-url",
                "http://mocked-front-url",
                tempDir.toString());
    }

    // ==========================
    // Tests deleteUsuarioChat()
    // ==========================
    @Test
    void deleteUsuarioChat_SuccessfulDeletion() throws SSLException, URISyntaxException {
        Long userId = 1L;
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Usuario chat borrado con exito!!!"));

        usuarioService.deleteUsuarioChat(userId);

        verify(webClientConfig).createSecureWebClient(anyString());
        verify(webClient).delete();
        verify(requestHeadersUriSpec).uri("/delete_usuario_chat/" + userId);
    }

    // ==========================
    // Tests createUsuario()
    // ==========================
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
        doNothing().when(spyService).createUsuarioChat(any(Usuario.class));
        doNothing().when(spyService).createUsuarioStream(any(Usuario.class));
        doNothing().when(spyService).updateSSR();

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
        verify(spyService).createUsuarioChat(usuarioInsertado);
        verify(spyService).createUsuarioStream(usuarioInsertado);
        verify(spyService).updateSSR();
    }

    @Test
    void createUsuario_ThrowsRepositoryException_WhenSavedUserHasNullId() throws InternalComunicationException {
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
    void createUsuario_WithFotoUsuario_AssignsCorrectly() throws RepositoryException, InternalComunicationException {
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
        doNothing().when(spyService).createUsuarioChat(any(Usuario.class));
        doNothing().when(spyService).createUsuarioStream(any(Usuario.class));
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
        doNothing().when(spyService).createUsuarioChat(any(Usuario.class));
        doNothing().when(spyService).createUsuarioStream(any(Usuario.class));
        doNothing().when(spyService).updateSSR();

        AuthResponse resp = spyService.createUsuario(newUsuario);

        assertNotNull(resp);
        assertTrue(resp.status());
        assertEquals(usuarioInsertado, resp.usuario());
        assertEquals(newUsuario.fotoUsuario(), resp.usuario().getFotoUsuario());
    }
}
