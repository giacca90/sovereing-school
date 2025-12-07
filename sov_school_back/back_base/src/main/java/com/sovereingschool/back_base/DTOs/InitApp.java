package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;
import java.util.List;

public record InitApp(
                List<CursosInit> cursosInit,
                List<ProfesInit> profesInit,
                Estadistica estadistica) implements Serializable {
}
