package com.sovereingschool.back_common.DTOs;

import java.io.Serializable;

public record ClaseDTO(
                Long idClase,
                String nombreClase,
                String descripcionClase,
                String contenidoClase,
                // 0 - ESTATICO, 1 - OBS - 2 - WEBCAM
                Integer tipoClase,
                String direccionClase,
                Integer posicionClase,
                Long cursoClase) implements Serializable {
}
