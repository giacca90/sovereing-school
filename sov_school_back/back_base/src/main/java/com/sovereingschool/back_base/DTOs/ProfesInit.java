package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;
import java.util.List;

public record ProfesInit(
        Long idUsuario,
        String nombreUsuario,
        List<String> fotoUsuario,
        String presentacion) implements Serializable {
}
