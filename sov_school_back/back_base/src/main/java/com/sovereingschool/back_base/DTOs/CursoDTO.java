package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public record CursoDTO(
        Long idCurso,
        String nombreCurso,
        List<Long> profesoresCurso,
        Date fechaPublicacionCurso,
        List<ClaseDTO> clasesCurso,
        List<Long> planesCurso,
        String descripcionCorta,
        String descripcionLarga,
        String imagenCurso,
        BigDecimal precioCurso) implements Serializable {
}
