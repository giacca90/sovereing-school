package com.sovereingschool.back_common.Repositories;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {

    @Query("SELECT c FROM Curso c")
    List<Curso> getAllCursos();

    @Query("SELECT c.nombreCurso FROM Curso c WHERE c.idCurso = :id")
    Optional<String> findNombreCursoById(@Param("id") Long id);

    @Query("SELECT u FROM Usuario u WHERE u IN (SELECT c.profesoresCurso FROM Curso c WHERE c.idCurso = :id)")
    List<Usuario> findProfesoresCursoById(@Param("id") Long id);

    @Query("SELECT c.fechaPublicacionCurso FROM Curso c WHERE c.idCurso = :id")
    Optional<Date> findFechaCreacionCursoById(@Param("id") Long id);

    @Query("SELECT cl FROM Clase cl WHERE cl.cursoClase.idCurso = :id")
    List<Clase> findClasesCursoById(@Param("id") Long id);

    @Query("SELECT p FROM Plan p WHERE p IN (SELECT c.planesCurso FROM Curso c WHERE c.idCurso = :id)")
    List<Plan> findPlanesCursoById(@Param("id") Long id);

    @Query("SELECT c.precioCurso FROM Curso c WHERE c.idCurso = :id")
    Optional<BigDecimal> findPrecioCursoById(@Param("id") Long id);

    @Query("SELECT c.imagenCurso FROM Curso c WHERE c.idCurso = :id")
    Optional<String> findImagenCursoById(@Param("id") Long id);
}
