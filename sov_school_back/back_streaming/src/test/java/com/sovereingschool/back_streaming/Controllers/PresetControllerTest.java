package com.sovereingschool.back_streaming.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_streaming.Models.Preset;
import com.sovereingschool.back_streaming.Models.Preset.PresetValue;
import com.sovereingschool.back_streaming.Services.UsuarioPresetsService;

/**
 * Pruebas unitarias para {@link PresetController}.
 */
@ExtendWith(MockitoExtension.class)
class PresetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioPresetsService usuarioPresetsService;

    @InjectMocks
    private PresetController presetController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(presetController).build();
    }

    /**
     * Prueba que el endpoint /presets/start devuelva true y un estado 200 OK cuando
     * la inicialización de presets es exitosa.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void startPresets_ShouldReturnTrueAndStatusOkOnSuccess() throws Exception {
        when(usuarioPresetsService.createPresetsForEligibleUsers()).thenReturn(true);

        mockMvc.perform(get("/presets/start"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * Prueba que el endpoint /presets/start devuelva false y un estado 500 Internal
     * Server Error cuando la inicialización de presets falla.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void startPresets_ShouldReturnFalseAndInternalServerErrorOnError() throws Exception {
        when(usuarioPresetsService.createPresetsForEligibleUsers()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/presets/start"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("false"));
    }

    /**
     * Prueba que el endpoint /presets/get/{idUsuario} devuelva los presets y un
     * estado 200 OK cuando se encuentran.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void getPresets_ShouldReturnPresetsAndStatusOkOnSuccess() throws Exception {
        final Long idUsuario = 1L;
        final Map<String, PresetValue> presetsMap = new HashMap<>();
        final PresetValue pv = new PresetValue();
        pv.setShortcut("S1");
        presetsMap.put("preset1", pv);

        final Preset preset = new Preset(idUsuario, presetsMap);
        when(usuarioPresetsService.getPresetsForUser(idUsuario)).thenReturn(preset);

        mockMvc.perform(get("/presets/get/" + idUsuario))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Prueba que el endpoint /presets/get/{idUsuario} devuelva un estado 404 Not
     * Found cuando no se encuentran presets.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void getPresets_ShouldReturnOkWhenNull() throws Exception {
        final Long idUsuario = 1L;
        when(usuarioPresetsService.getPresetsForUser(idUsuario)).thenReturn(null);

        mockMvc.perform(get("/presets/get/" + idUsuario))
                .andExpect(status().isOk());
    }

    /**
     * Prueba que el endpoint /presets/get/{idUsuario} devuelva un estado 500
     * Internal Server Error cuando ocurre un error en el servicio.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void getPresets_ShouldReturnInternalServerErrorOnError() throws Exception {
        final Long idUsuario = 1L;
        when(usuarioPresetsService.getPresetsForUser(idUsuario)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/presets/get/" + idUsuario))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Prueba que el endpoint /presets/update/{idUsuario} devuelva true y un estado
     * 200 OK cuando la actualización de presets es exitosa.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void updatePresets_ShouldReturnOkOnSuccess() throws Exception {
        final Long idUsuario = 1L;
        final Map<String, PresetValue> presetsMap = new HashMap<>();
        final PresetValue pv = new PresetValue();
        pv.setShortcut("S1");
        presetsMap.put("preset1", pv);

        mockMvc.perform(put("/presets/save/" + idUsuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presetsMap)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * Prueba que el endpoint /presets/update/{idUsuario} devuelva un estado 500
     * Internal Server Error cuando ocurre un error en el servicio.
     * 
     * @throws Exception si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void updatePresets_ShouldReturnInternalServerErrorOnError() throws Exception {
        final Long idUsuario = 1L;
        final Map<String, PresetValue> presetsMap = new HashMap<>();
        final PresetValue pv = new PresetValue();
        pv.setShortcut("S1");
        presetsMap.put("preset1", pv);

        doThrow(new RuntimeException("Error al guardar")).when(usuarioPresetsService).savePresetsForUser(eq(idUsuario),
                any(Map.class));

        mockMvc.perform(put("/presets/save/" + idUsuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presetsMap)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("false"));
    }
}