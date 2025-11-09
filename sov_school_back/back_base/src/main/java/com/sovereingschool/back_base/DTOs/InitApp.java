package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;
import java.util.List;

public record InitApp(
        List<CursosInit> CursosInit,
        List<ProfesInit> ProfesInit,
        Estadistica estadistica) implements Serializable {
}
