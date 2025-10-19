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
    private String id_mensaje;
    private Long id_curso;
    private Long id_clase;
    private Long id_usuario;

    private String nombre_curso;
    private String nombre_clase;
    private String nombre_usuario;

    private String foto_curso;
    private String foto_usuario;

    private MensajeChatDTO respuesta;
    private Integer pregunta;

    private String mensaje;

    private Date fecha;

}