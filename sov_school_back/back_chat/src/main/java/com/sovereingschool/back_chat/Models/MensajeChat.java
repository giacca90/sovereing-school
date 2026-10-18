package com.sovereingschool.back_chat.Models;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@Document(collection = "messages_chat")
public class MensajeChat {
    @Id
    private String id;

    /** Curso ID */
    private Long idCurso;

    /** Clase ID */
    private Long idClase;

    /** Usuario ID de quien envía el mensaje */
    private Long idUsuario;

    /** ID del documento de respuesta (opcional) */
    private String respuesta;

    /** Segundo de video en la cuel se hizo la pregunta */
    private Integer momento;

    /** Texto del mensaje */
    private String mensaje;

    /** Fecha de envío del mensaje */
    private Date fecha;

    public MensajeChat(String id, Long idCurso, Long idClase, Long idUsuario, String respuesta, Integer momento,
            String mensaje, Date fecha) {
        this.id = id;
        this.idCurso = idCurso;
        this.idClase = idClase;
        this.idUsuario = idUsuario;
        this.respuesta = respuesta;
        this.momento = momento;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

}