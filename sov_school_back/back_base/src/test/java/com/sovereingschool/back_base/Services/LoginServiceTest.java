package com.sovereingschool.back_base.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    }

    @Mock
    private LoginRepository loginRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(
                loginRepository,
                usuarioRepository,
                passwordEncoder,
                jwtUtil);
    }
}
