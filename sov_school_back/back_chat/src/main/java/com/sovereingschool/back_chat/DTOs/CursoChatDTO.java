package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.List;

public record CursoChatDTO(
        Long idCurso,
        List<ClaseChatDTO> clases,
        List<MensajeChatDTO> mensajes,
        String nombreCurso,
        String fotoCurso) implements Serializable {
}
