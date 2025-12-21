package com.sovereingschool.back_base.Services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    // ==========================
    // Tests compruebaCorreo()
    // ==========================
    @Nested
    class CompruebaCorreoTests {
        @Test
        void compruebaCorreoTest() {
            String correo = "correo@ejemplo.com";
            when(loginRepository.compruebaCorreo(correo)).thenReturn(Optional.of(1L));

            assertEquals(1L, loginService.compruebaCorreo(correo));
            verify(loginRepository).compruebaCorreo(correo);
        }

        @Test
        void compruebaCorreoTest_Empty() {
            String correo = "correo@ejemplo.com";
            when(loginRepository.compruebaCorreo(correo)).thenReturn(Optional.empty());

            assertEquals(0L, loginService.compruebaCorreo(correo));
            verify(loginRepository).compruebaCorreo(correo);
        }

        @Test
        void compruebaCorreoTest_Error() {
            String correo = "correo@ejemplo.com";
            when(loginRepository.compruebaCorreo(correo)).thenThrow(new RuntimeException("Error"));

            assertThrows(RuntimeException.class, () -> loginService.compruebaCorreo(correo));
            verify(loginRepository).compruebaCorreo(correo);
        }
    }

    // ==========================
    // Tests createNuevoLogin()
    // ==========================
    @Nested
    class CreateNuevoLoginTests {
    }

    // ==========================
    // Tests getCorreoLogin()
    // ==========================
    @Nested
    class GetCorreoLoginTests {
    }

    // ==========================
    // Tests getPasswordLogin()
    // ==========================
    @Nested
    class GetPasswordLoginTests {
    }

    // ==========================
    // Tests changeCorreoLogin()
    // ==========================
    @Nested
    class ChangeCorreoLoginTests {
    }

    // ==========================
    // Tests changePasswordLogin()
    // ==========================
    @Nested
    class ChangePasswordLoginTests {
    }

    // ==========================
    // Tests deleteLogin()
    // ==========================
    @Nested
    class DeleteLoginTests {
    }

    // ==========================
    // Tests loadUserByUsername()
    // ==========================
    @Nested
    class LoadUserByUsernameTests {

        @Test
        void loadUserByUsernameTest_ExistingUser() {
            Login login = new Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("user@example.com");
            login.setPassword("encodedPassword");
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setNombreUsuario("Tester");
            usuario.setRollUsuario(RoleEnum.USER);
            usuario.setIsEnabled(true);
            usuario.setAccountNoExpired(true);
            usuario.setCredentialsNoExpired(true);
            usuario.setAccountNoLocked(true);
            when(loginRepository.getLoginForCorreo("user@example.com")).thenReturn(Optional.of(login));
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertDoesNotThrow(() -> loginService.loadUserByUsername("user@example.com"));
        }

        @Test
        void loadUserByUsernameTest_NonExistingUser() {
            when(loginRepository.getLoginForCorreo("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> loginService.loadUserByUsername("nonexistent@example.com"));
        }
    }

    // ==========================
    // Tests loginUser()
    // ==========================
    @Nested
    class LoginUserTests {
    }

    // ==========================
    // Tests refreshAccessToken()
    // ==========================
    @Nested
    class RefreshAccessTokenTests {
    }

    // ==========================
    // Tests loginWithToken()
    // ==========================
    @Nested
    class LoginWithTokenTests {
        // Test para loginWithToken con token válido
        @Test
        void testLoginWithToken_ValidToken() {
            Usuario usuario = new Usuario();
            usuario.setRollUsuario(RoleEnum.USER);
            usuario.setIsEnabled(true);
            when(jwtUtil.getIdUsuario("validtoken")).thenReturn(1L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertDoesNotThrow(() -> loginService.loginWithToken("validtoken"));
        }

        // Test para loginWithToken con token inválido
        @Test
        void testLoginWithToken_InvalidToken() {
            when(jwtUtil.getIdUsuario("invalidtoken"))
                    .thenThrow(new com.auth0.jwt.exceptions.JWTVerificationException("Invalid token"));

            assertThrows(com.auth0.jwt.exceptions.JWTVerificationException.class,
                    () -> loginService.loginWithToken("invalidtoken"));
        }
    }

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtUtil jwtUtil;

    private LoginService loginService;

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(loginRepository, usuarioRepository, passwordEncoder, jwtUtil);
    }
}
