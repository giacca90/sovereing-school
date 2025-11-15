package com.sovereingschool.back_streaming.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_streaming.Repositories.PresetRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioPresetsServiceTest {

    // ==========================
    // Tests createPresetsForEligibleUsers()
    // ==========================
    @Nested
    class CreatePresetsForEligibleUsersTests {
    }

    // ==========================
    // Tests getPresetsForUser()
    // ==========================
    @Nested
    class GetPresetsForUserTests {
    }

    // ==========================
    // Tests savePresetsForUser()
    // ==========================
    @Nested
    class SavePresetsForUserTests {
    }

    // ==========================
    // Tests deletePresetsForUser()
    // ==========================
    @Nested
    class DeletePresetsForUserTests {
    }

    @Mock
    private PresetRepository presetRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioPresetsService usuarioPresetsService;

    @BeforeEach
    void setUp() {
        usuarioPresetsService = new UsuarioPresetsService(presetRepository, usuarioRepository);
    }
}
