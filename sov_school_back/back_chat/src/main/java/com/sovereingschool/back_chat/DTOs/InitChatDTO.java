package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.List;

public record InitChatDTO(
        Long idUsuario,
        List<MensajeChatDTO> mensajes,
        List<CursoChatDTO> cursos) implements Serializable {
}