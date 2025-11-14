package com.sovereingschool.back_chat.Models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaseChat {

    /** Clase ID */
    private Long idClase;

    /** Curso ID */
    private Long idCurso;

    /** Lista de ids de documentos de mensajes */
    private List<String> mensajes;

}