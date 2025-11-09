package com.sovereingschool.back_streaming.Services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_streaming.Models.Preset;
import com.sovereingschool.back_streaming.Repositories.PresetRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UsuarioPresetsService {

    private PresetRepository presetRepository;
    private UsuarioRepository usuarioRepository;

    private Logger logger = LoggerFactory.getLogger(UsuarioPresetsService.class);

    /**
     * Constructor de UsuarioPresetsService
     *
     * @param presetRepository  Repositorio de presets
     * @param usuarioRepository Repositorio de usuarios
     */
    public UsuarioPresetsService(PresetRepository presetRepository, UsuarioRepository usuarioRepository) {
        this.presetRepository = presetRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Función para crear los presets para los usuarios elegibles
     * 
     * @return Booleano con el resultado de la operación
     */
    public boolean createPresetsForEligibleUsers() {
        try {
            this.usuarioRepository.findAll().forEach((Usuario user) -> {
                if (user.getRollUsuario().name().equals("PROFESOR")
                        || user.getRollUsuario().name().equals("ADMIN")) {
                    this.presetRepository.save(new Preset(user.getIdUsuario()));
                }
            });
            return true;
        } catch (Exception e) {
            logger.error("Error en crear DDBB de presets: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Función para obtener los presets del usuario
     * 
     * @param idUsuario ID del usuario
     * @return Preset con los datos del usuario
     */
    public Preset getPresetsForUser(Long idUsuario) {
        return this.presetRepository.findByIdUsuario(idUsuario).orElse(null);
    }

    /**
     * Función para guardar los presets del usuario
     * 
     * @param idUsuario ID del usuario
     * @param presets   Mapa de PresetValue
     */
    public void savePresetsForUser(Long idUsuario, Map<String, Preset.PresetValue> presets) {
        this.deletePresetsForUser(idUsuario);
        this.presetRepository.save(new Preset(idUsuario, presets));
    }

    /**
     * Función para eliminar los presets del usuario
     * 
     * @param idUsuario ID del usuario
     */
    public void deletePresetsForUser(Long idUsuario) {
        this.presetRepository.deleteByIdUsuario(idUsuario);
    }

}
