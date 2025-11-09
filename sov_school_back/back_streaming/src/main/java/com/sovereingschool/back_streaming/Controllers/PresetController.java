package com.sovereingschool.back_streaming.Controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_streaming.Models.Preset;
import com.sovereingschool.back_streaming.Models.Preset.PresetValue;
import com.sovereingschool.back_streaming.Services.UsuarioPresetsService;

@RestController
@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
@RequestMapping("/presets")
public class PresetController {

    private UsuarioPresetsService usuarioPresetsService;

    private Logger logger = LoggerFactory.getLogger(PresetController.class);

    /**
     * Constructor de PresetController
     *
     * @param usuarioPresetsService Servicio de presets
     */
    public PresetController(UsuarioPresetsService usuarioPresetsService) {
        this.usuarioPresetsService = usuarioPresetsService;
    }

    /**
     * Función para iniciar el servicio de presets
     *
     * @return ResponseEntity<Boolean> con el resultado de la operación
     */
    @GetMapping("/start")
    public ResponseEntity<Boolean> getMethodName() {
        try {
            Boolean res = this.usuarioPresetsService.createPresetsForEligibleUsers();
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error en crear DDBB de presets: {}", e.getMessage());
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para obtener los presets del usuario
     *
     * @param idUsuario ID del usuario
     * @return ResponseEntity<Map<String, PresetValue>> con los presets del usuario
     */
    @GetMapping("/get/{idUsuario}")
    public ResponseEntity<?> getPresets(@PathVariable Long idUsuario) {
        try {
            Preset result = this.usuarioPresetsService.getPresetsForUser(idUsuario);
            if (result == null) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            Map<String, PresetValue> presetsMap = result.getPresets();
            return new ResponseEntity<>(presetsMap, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error en obtener presets para usuario {}: {}", idUsuario, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para actualizar los presets del usuario
     *
     * @param data      String con los datos del usuario
     * @param idUsuario ID del usuario
     * @return ResponseEntity<Boolean> con el resultado de la operación
     */
    @PutMapping("/save/{idUsuario}")
    public ResponseEntity<?> update(@RequestBody String data, @PathVariable Long idUsuario) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, PresetValue> presetsMap = objectMapper.readValue(data,
                    new TypeReference<Map<String, PresetValue>>() {
                    });
            this.usuarioPresetsService.savePresetsForUser(idUsuario, presetsMap);
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error en actualizar presets para usuario {}: {}", idUsuario, e.getMessage());
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
