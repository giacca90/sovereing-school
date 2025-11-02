package com.sovereingschool.back_base.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursosInit {

    private Long idCurso;

    private String nombreCurso;

    private List<Long> profesoresCurso;

    private String descriccionCorta;

    private String imagenCurso;

}
