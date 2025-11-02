package com.sovereingschool.back_chat.Models;

import java.io.Serializable;
import java.util.List;

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
@Document(collection = "courses_chat")
public class CursoChat implements Serializable {
    @Id
    private String id;

    private Long idCurso;

    private List<ClaseChat> clases;

    private List<String> mensajes;

    private String ultimo;

}