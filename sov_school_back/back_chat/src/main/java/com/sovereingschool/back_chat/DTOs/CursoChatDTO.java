package com.sovereingschool.back_chat.DTOs;

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
public class CursoChatDTO {

    private Long idCurso;

    private List<ClaseChatDTO> clases;

    private List<MensajeChatDTO> mensajes;

    private String nombreCurso;

    private String fotoCurso;
}
