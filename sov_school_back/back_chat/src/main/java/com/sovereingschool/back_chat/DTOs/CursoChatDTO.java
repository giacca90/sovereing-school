package com.sovereingschool.back_chat.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoChatDTO {

    private Long idCurso;

    private List<ClaseChatDTO> clases;

    private List<MensajeChatDTO> mensajes;

    private String nombreCurso;

    private String fotoCurso;
}
