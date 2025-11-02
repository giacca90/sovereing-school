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
public class ProfesInit {
    private Long idUsuario;

    private String nombreUsuario;

    private List<String> fotoUsuario;

    private String presentacion;

}
