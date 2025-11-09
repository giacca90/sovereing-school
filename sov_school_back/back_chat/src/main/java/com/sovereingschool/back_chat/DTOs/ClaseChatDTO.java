package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.List;

public record ClaseChatDTO(
        Long idClase,
        Long idCurso,
        String nombreClase,
        List<MensajeChatDTO> mensajes) implements Serializable {
}
