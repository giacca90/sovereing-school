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
public class InitApp {
    private List<CursosInit> CursosInit;

    private List<ProfesInit> ProfesInit;

    private Estadistica estadistica;

}
