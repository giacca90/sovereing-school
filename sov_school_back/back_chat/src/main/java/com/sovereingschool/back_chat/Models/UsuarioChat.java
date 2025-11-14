package com.sovereingschool.back_chat.Models;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users_chat")
public class UsuarioChat implements Serializable {
    @Id
    private String id;

    /** Usuario ID */
    private Long idUsuario;

    /** Lista de ids de documentos de cursos */
    private List<String> cursos;

    /** Lista de ids de documentos de mensajes */
    private List<String> mensajes;

}