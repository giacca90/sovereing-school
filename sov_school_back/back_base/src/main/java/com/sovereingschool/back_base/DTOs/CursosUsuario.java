package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;
import java.util.List;

public record CursosUsuario(
        Long idUsuario,
        List<Long> idsCursos) implements Serializable {
}
