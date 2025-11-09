package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;

public record Estadistica(
        int profesores,
        Long alumnos,
        Long cursos,
        Long clases) implements Serializable {
}
