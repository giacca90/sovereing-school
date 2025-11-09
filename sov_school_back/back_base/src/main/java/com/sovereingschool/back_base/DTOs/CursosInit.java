package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;
import java.util.List;

public record CursosInit(
        Long idCurso,
        String nombreCurso,
        List<Long> profesoresCurso,
        String descriccionCorta,
        String imagenCurso) implements Serializable {
}
