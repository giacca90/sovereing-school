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

    public Preset getPresetsForUser(Long idUsuario) {
        return this.presetRepository.findByIdUsuario(idUsuario).orElse(null);
    }

    public void savePresetsForUser(Long idUsuario, Map<String, Preset.PresetValue> presets) {
        this.deletePresetsForUser(idUsuario);
        this.presetRepository.save(new Preset(idUsuario, presets));
    }

    public void deletePresetsForUser(Long idUsuario) {
        this.presetRepository.deleteByIdUsuario(idUsuario);
    }

}
