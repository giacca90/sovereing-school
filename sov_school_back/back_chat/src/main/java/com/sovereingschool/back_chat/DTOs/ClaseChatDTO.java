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
public class ClaseChatDTO {

    private Long idClase;

    private Long idCurso;

    private String nombreClase;

    private List<MensajeChatDTO> mensajes;
}
