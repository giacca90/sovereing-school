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

    private Long idUsuario;

    private List<String> cursos;

    private List<String> mensajes;

}