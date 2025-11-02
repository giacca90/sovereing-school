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

    private Long idClase;

    private Long idCurso;

    private List<String> mensajes;

}