package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@ToString
public class InitChatDTO implements Serializable {
    private Long idUsuario;

    private List<MensajeChatDTO> mensajes;

    private List<CursoChatDTO> cursos;
}