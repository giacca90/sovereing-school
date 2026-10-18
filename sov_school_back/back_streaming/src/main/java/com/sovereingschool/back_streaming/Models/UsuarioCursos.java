package com.sovereingschool.back_streaming.Models;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sovereingschool.back_common.Models.RoleEnum;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@Document(collection = "user_courses")
@CompoundIndex(name = "user_courses_idx", def = "{'idUsuario': 1}", unique = true)
public class UsuarioCursos implements Serializable {
    @Id
    private String id;

    /** Usuario ID */
    @Indexed
    private Long idUsuario;

    /** Rol del usuario */
    private RoleEnum rolUsuario;

    /** Lista de StatusCurso */
    private List<StatusCurso> cursos;

    public UsuarioCursos(String id, Long idUsuario, RoleEnum rolUsuario, List<StatusCurso> cursos) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.rolUsuario = rolUsuario;
        this.cursos = cursos;
    }
}
