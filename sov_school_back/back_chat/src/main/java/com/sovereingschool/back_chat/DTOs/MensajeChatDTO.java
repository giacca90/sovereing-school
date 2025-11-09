package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.Date;

public record MensajeChatDTO(
        String idMensaje,
        Long idCurso,
        Long idClase,
        Long idUsuario,
        String nombreCurso,
        String nombreClase,
        String nombreUsuario,
        String fotoCurso,
        String fotoUsuario,
        MensajeChatDTO respuesta,
        Integer pregunta,
        String mensaje,
        Date fecha) implements Serializable {
}