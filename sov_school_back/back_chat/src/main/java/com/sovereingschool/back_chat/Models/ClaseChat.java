package com.sovereingschool.back_chat.Models;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class ClaseChat {

    /** Clase ID */
    private Long idClase;

    /** Curso ID */
    private Long idCurso;

    /** Lista de ids de documentos de mensajes */
    private List<String> mensajes;

    public ClaseChat(Long idClase, Long idCurso, List<String> mensajes) {
        this.idClase = idClase;
        this.idCurso = idCurso;
        this.mensajes = mensajes;
    }

}