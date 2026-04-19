package com.sovereingschool.back_streaming.Models;

import java.util.List;

// Importaciones específicas para MongoDB
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "status_cursos") // Define la colección en Mongo
// Creamos un índice compuesto: idUsuario + idCurso.
// Esto hace que buscar el progreso de un alumno en un curso sea ultra rápido.
@CompoundIndex(name = "user_course_idx", def = "{'idUsuario': 1, 'idCurso': 1}", unique = true)
public class StatusCurso {

    @Id
    private String id; // El ID interno de MongoDB (se genera automáticamente)

    private Long idUsuario; // Necesario para saber de quién es el progreso

    private Long idCurso; // El ID del curso al que pertenece este progreso

    // Eliminamos @ElementCollection porque Mongo guarda las listas de forma nativa
    private List<StatusClase> clases;
}