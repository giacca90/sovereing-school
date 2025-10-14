package com.sovereingschool.back_streaming.Controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UsuarioPresetsService usuarioPresetsService;

    @GetMapping("/start")
    public ResponseEntity<Boolean> getMethodName() {
        try {
            Boolean res = this.usuarioPresetsService.createPresetsForEligibleUsers();
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error en crear DDBB de presets: " + e.getMessage());
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{id_usuario}")
    public ResponseEntity<?> getPresets(@PathVariable Long id_usuario) {
        try {
            Preset result = this.usuarioPresetsService.getPresetsForUser(id_usuario);
            if (result == null) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            Map<String, PresetValue> presetsMap = result.getPresets();
            return new ResponseEntity<>(presetsMap, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error en obtener presets para usuario " + id_usuario + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/save/{id_usuario}")
    public ResponseEntity<?> update(@RequestBody String data, @PathVariable Long id_usuario) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, PresetValue> presetsMap = objectMapper.readValue(data,
                    new TypeReference<Map<String, PresetValue>>() {
                    });
            this.usuarioPresetsService.savePresetsForUser(id_usuario, presetsMap);
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error en actualizar presets para usuario " + id_usuario + ": " + e.getMessage());
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
