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

import com.sovereingschool.back_base.DTOs.ChangePassword;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.persistence.EntityNotFoundException;

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
        @Test
        void createNuevoLoginTest() {
            Login login = new Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("correo@ejemplo.com");
            login.setPassword("password");
            when(loginRepository.save(login)).thenReturn(login);

            assertEquals("Nuevo Usuario creado con éxito!!!", loginService.createNuevoLogin(login));
            verify(loginRepository).save(login);
        }

        @Test
        void createNuevoLoginTest_Error() {
            Login login = new Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("correo@ejemplo.com");
            login.setPassword("password");
            when(loginRepository.save(login)).thenThrow(new RuntimeException("Error"));

            assertThrows(RuntimeException.class, () -> loginService.createNuevoLogin(login));
            verify(loginRepository).save(login);
        }
    }

    // ==========================
    // Tests getCorreoLogin()
    // ==========================
    @Nested
    class GetCorreoLoginTests {
        @Test
        void getCorreoLoginTest() {
            String correo = "correo@ejemplo.com";
            Long idUsuario = 1L;
            when(loginRepository.findCorreoLoginForId(idUsuario)).thenReturn(Optional.of(correo));

            assertEquals(correo, loginService.getCorreoLogin(1L));
            verify(loginRepository).findCorreoLoginForId(idUsuario);
        }

        @Test
        void getCorreoLoginTest_Empty() {
            Long idUsuario = 1L;
            when(loginRepository.findCorreoLoginForId(idUsuario)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loginService.getCorreoLogin(1L));
            verify(loginRepository).findCorreoLoginForId(idUsuario);
        }

        @Test
        void getCorreoLoginTest_Error() {
            Long idUsuario = 1L;
            when(loginRepository.findCorreoLoginForId(idUsuario)).thenThrow(new RuntimeException("Error"));

            assertThrows(RuntimeException.class, () -> loginService.getCorreoLogin(1L));
            verify(loginRepository).findCorreoLoginForId(idUsuario);
        }
    }

    // ==========================
    // Tests getPasswordLogin()
    // ==========================
    @Nested
    class GetPasswordLoginTests {

        @Test
        void getPasswordLoginTest() {
            String password = "password";
            Long idUsuario = 1L;
            when(loginRepository.findPasswordLoginForId(idUsuario)).thenReturn(Optional.of(password));

            assertEquals(password, loginService.getPasswordLogin(1L));
            verify(loginRepository).findPasswordLoginForId(idUsuario);
        }

        @Test
        void getPasswordLoginTest_Empty() {
            Long idUsuario = 1L;
            when(loginRepository.findPasswordLoginForId(idUsuario)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loginService.getPasswordLogin(1L));
            verify(loginRepository).findPasswordLoginForId(idUsuario);
        }

        @Test
        void getPasswordLoginTest_Error() {
            Long idUsuario = 1L;
            when(loginRepository.findPasswordLoginForId(idUsuario)).thenThrow(new RuntimeException("Error"));

            assertThrows(RuntimeException.class, () -> loginService.getPasswordLogin(1L));
            verify(loginRepository).findPasswordLoginForId(idUsuario);
        }
    }

    // ==========================
    // Tests changeCorreoLogin()
    // ==========================
    @Nested
    class ChangeCorreoLoginTests {

        @Test
        void changeCorreoLoginTest() {
            Login login = new Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("correo@ejemplo.com");
            login.setPassword("password");
            when(loginRepository.changeCorreoLoginForId(1L, "correo@ejemplo.com")).thenReturn(Optional.of(1));

            assertEquals("Correo cambiado con éxito!!!", loginService.changeCorreoLogin(login));
            verify(loginRepository).changeCorreoLoginForId(1L, "correo@ejemplo.com");
        }

        @Test
        void changeCorreoLoginTest_Error() {
            Login login = new Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("correo@ejemplo.com");
            login.setPassword("password");
            when(loginRepository.changeCorreoLoginForId(1L, "correo@ejemplo.com"))
                    .thenThrow(new RuntimeException("Error"));

            assertThrows(RuntimeException.class, () -> loginService.changeCorreoLogin(login));
            verify(loginRepository).changeCorreoLoginForId(1L, "correo@ejemplo.com");
        }
    }

    // ==========================
    // Tests changePasswordLogin()
    // ==========================
    @Nested
    class ChangePasswordLoginTests {

        @Test
        void changePasswordLoginTest() {
            ChangePassword changepassword = new ChangePassword(
                    1L,
                    "password",
                    "newPassword");

            when(loginRepository.findPasswordLoginForId(1L)).thenReturn(Optional.of("password"));
            when(loginRepository.changePasswordLoginForId(1L, "newPassword")).thenReturn(Optional.of(1));

            assertEquals(1, loginService.changePasswordLogin(changepassword));
            verify(loginRepository).findPasswordLoginForId(1L);
            verify(loginRepository).changePasswordLoginForId(1L, "newPassword");
        }

        @Test
        void changePasswordLoginTest_newPasswordEmpty() {
            ChangePassword changepassword = new ChangePassword(
                    1L,
                    "",
                    "newPassword");

            assertEquals(null, loginService.changePasswordLogin(changepassword));
        }

        @Test
        void changePasswordLoginTest_oldPasswordEmpty() {
            ChangePassword changepassword = new ChangePassword(
                    1L,
                    "password",
                    "");

            assertEquals(null, loginService.changePasswordLogin(changepassword));
        }

        @Test
        void changePasswordLoginTest_differentOldPassword() {
            ChangePassword changepassword = new ChangePassword(
                    1L,
                    "password",
                    "newPassword");

            when(loginRepository.findPasswordLoginForId(1L)).thenReturn(Optional.of("differentPassword"));

            assertEquals(0, loginService.changePasswordLogin(changepassword));
        }

    }

    // ==========================
    // Tests deleteLogin()
    // ==========================
    @Nested
    class DeleteLoginTests {
        @Test
        void deleteLoginTest() {
            Long idUsuario = 1L;
            assertEquals("Login eliminado con éxito!!!", loginService.deleteLogin(idUsuario));
            verify(loginRepository).deleteById(idUsuario);
        }

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

        @Test
        void loadUserByUsernameTest_Visitante() {
            assertEquals("Visitante", loginService.loadUserByUsername("Visitante").getUsername());
            assertEquals("visitante", loginService.loadUserByUsername("Visitante").getPassword());
        }

        @Test
        void loadUserByUsernameTest_UserNotFound() {
            Login login = new Login();
            login.setIdUsuario(1L);
            login.setCorreoElectronico("user@example.com");
            login.setPassword("encodedPassword");
            when(loginRepository.getLoginForCorreo("user@example.com")).thenReturn(Optional.of(login));
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> loginService.loadUserByUsername("user@example.com"));
        }
    }

    // ==========================
    // Tests loginUser()
    // ==========================
    @Nested
    class LoginUserTests {
        private Long idUsuario;
        private String correo;
        private String password;
        private Usuario usuario;
        private Login login;

        @BeforeEach
        void setUp() {
            idUsuario = 1L;
            correo = "correo@ejemplo.com";
            password = "password";
            login = new Login();
            login.setIdUsuario(idUsuario);
            login.setCorreoElectronico(correo);
            login.setPassword(password);
            usuario = new Usuario();
            usuario.setIdUsuario(idUsuario);
            usuario.setNombreUsuario("Tester");
            usuario.setRollUsuario(RoleEnum.USER);
            usuario.setIsEnabled(true);
            usuario.setAccountNoExpired(true);
            usuario.setCredentialsNoExpired(true);
            usuario.setAccountNoLocked(true);
        }
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
