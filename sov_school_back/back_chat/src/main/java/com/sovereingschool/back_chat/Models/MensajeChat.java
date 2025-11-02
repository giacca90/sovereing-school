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

    private Long idCurso;

    private Long idClase;

    private Long idUsuario;

    private String respuesta;

    private Integer momento;

    private String mensaje;

    private Date fecha;

}