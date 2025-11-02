package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitChatDTO implements Serializable {
    private Long idUsuario;

    private List<MensajeChatDTO> mensajes;

    private List<CursoChatDTO> cursos;
}