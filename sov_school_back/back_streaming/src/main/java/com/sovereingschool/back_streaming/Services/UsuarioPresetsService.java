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

    public UsuarioPresetsService(PresetRepository presetRepository, UsuarioRepository usuarioRepository) {
        this.presetRepository = presetRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public boolean createPresetsForEligibleUsers() {
        try {
            this.usuarioRepository.findAll().forEach((Usuario user) -> {
                if (user.getRoll_usuario().name().equals("PROFESOR")
                        || user.getRoll_usuario().name().equals("ADMIN")) {
                    this.presetRepository.save(new Preset(user.getId_usuario()));
                }
            });
            return true;
        } catch (Exception e) {
            logger.error("Error en crear DDBB de presets: {}", e.getMessage());
            return false;
        }
    }

    public Preset getPresetsForUser(Long id_usuario) {
        return this.presetRepository.findByIdUsuario(id_usuario).orElse(null);
    }

    public void savePresetsForUser(Long id_usuario, Map<String, Preset.PresetValue> presets) {
        this.deletePresetsForUser(id_usuario);
        this.presetRepository.save(new Preset(id_usuario, presets));
    }

    public void deletePresetsForUser(Long id_usuario) {
        this.presetRepository.deleteByIdUsuario(id_usuario);
    }

}
