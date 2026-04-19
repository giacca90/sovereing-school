package com.sovereingschool.back_streaming.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_streaming.Models.Preset;
import com.sovereingschool.back_streaming.Repositories.PresetRepository;

/**
 * Pruebas unitarias para {@link UsuarioPresetsService}.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioPresetsServiceTest {

    @Nested
    class CreatePresetsForEligibleUsersTests {
        /**
         * Prueba la creación exitosa de presets para usuarios elegibles.
         */
        @Test
        void createPresetsForEligibleUsers_ShouldReturnTrueOnSuccess() {
            final Usuario admin = new Usuario();
            admin.setIdUsuario(1L);
            admin.setRollUsuario(RoleEnum.ADMIN);

            final Usuario user = new Usuario();
            user.setIdUsuario(2L);
            user.setRollUsuario(RoleEnum.USER);

            when(usuarioRepository.findAll()).thenReturn(List.of(admin, user));

            final boolean result = usuarioPresetsService.createPresetsForEligibleUsers();

            assertTrue(result, "La creación debería ser exitosa");
            verify(presetRepository, times(1)).save(any(Preset.class));
        }

        /**
         * Prueba el error al crear presets para usuarios elegibles.
         */
        @Test
        void createPresetsForEligibleUsers_ShouldReturnFalseOnException() {
            when(usuarioRepository.findAll()).thenThrow(new RuntimeException("DB error"));
            final boolean result = usuarioPresetsService.createPresetsForEligibleUsers();
            assertFalse(result, "La creación debería fallar");
        }
    }

    @Nested
    class GetPresetsForUserTests {
        /**
         * Prueba la obtención exitosa de presets para un usuario.
         */
        @Test
        void getPresetsForUser_ShouldReturnPresetWhenFound() {
            final Preset preset = new Preset(1L);
            when(presetRepository.findByIdUsuario(1L)).thenReturn(Optional.of(preset));

            final Preset result = usuarioPresetsService.getPresetsForUser(1L);
            assertNotNull(result, "El preset no debería ser nulo");
            assertEquals(1L, result.getIdUsuario(), "El ID de usuario debería coincidir");
        }

        /**
         * Prueba la obtención de presets cuando no se encuentran para un usuario.
         */
        @Test
        void getPresetsForUser_ShouldReturnNullWhenNotFound() {
            when(presetRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());
            assertNull(usuarioPresetsService.getPresetsForUser(1L), "El resultado debería ser nulo");
        }
    }

    @Nested
    class SavePresetsForUserTests {
        /**
         * Prueba el guardado de presets para un usuario.
         */
        @Test
        void savePresetsForUser_ShouldSaveCorrectly() {
            final Map<String, Preset.PresetValue> presetsMap = Collections.emptyMap();
            usuarioPresetsService.savePresetsForUser(1L, presetsMap);

            verify(presetRepository).deleteByIdUsuario(1L);
            verify(presetRepository).save(any(Preset.class));
        }
    }

    @Nested
    class DeletePresetsForUserTests {
        /**
         * Prueba la eliminación de presets para un usuario.
         */
        @Test
        void testDeletePresetsForUser() {
            usuarioPresetsService.deletePresetsForUser(1L);
            verify(presetRepository).deleteByIdUsuario(1L);
        }
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
