package com.sovereingschool.back_streaming.Models;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.sovereingschool.back_common.Models.RoleEnum;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_courses")
public class UsuarioCursos implements Serializable {
    @Id
    private String id;

    /** Usuario ID */
    private Long idUsuario;

    /** Rol del usuario */
    private RoleEnum rolUsuario;

    /** Lista de StatusCurso */
    private List<StatusCurso> cursos;
}
