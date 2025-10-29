package com.sovereingschool.back_chat.DTOs;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@ToString
public class MensajeChatDTO implements Serializable {
    private String idMensaje;
    private Long idCurso;
    private Long idClase;
    private Long idUsuario;

    private String nombreCurso;
    private String nombreClase;
    private String nombreUsuario;

    private String fotoCurso;
    private String fotoUsuario;

    private MensajeChatDTO respuesta;
    private Integer pregunta;

    private String mensaje;

    private Date fecha;

}