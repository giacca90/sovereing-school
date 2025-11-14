package com.sovereingschool.back_chat.Models;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

}