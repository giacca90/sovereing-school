package com.sovereingschool.back_base.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CursosInit {

    private Long idCurso;

    private String nombreCurso;

    private List<Long> profesoresCurso;

    private String descriccionCorta;

    private String imagenCurso;

}
