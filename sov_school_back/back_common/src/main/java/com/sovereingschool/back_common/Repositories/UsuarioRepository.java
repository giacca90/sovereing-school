package com.sovereingschool.back_common.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;

import jakarta.transaction.Transactional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN FETCH u.cursosUsuario
            WHERE u.idUsuario = :id
            """)
    Optional<Usuario> findUsuarioForId(@Param("id") Long id);

    @Query("SELECT u.nombreUsuario FROM Usuario u WHERE u.idUsuario = :id")
    Optional<String> findNombreUsuarioForId(@Param("id") Long id);

    @Query("SELECT u.fotoUsuario FROM Usuario u WHERE u.idUsuario = :id")
    List<String> findFotosUsuarioForId(@Param("id") Long id);

    @Query("SELECT u.rollUsuario FROM Usuario u WHERE u.idUsuario = :id")
    Optional<RoleEnum> findRollUsuarioForId(@Param("id") Long id);

    @Query("SELECT p FROM Plan p WHERE p.idPlan = (SELECT u.planUsuario.idPlan FROM Usuario u WHERE u.idUsuario = :id)")
    Optional<Plan> findPlanUsuarioForId(@Param("id") Long id);

    @Query("SELECT u.cursosUsuario FROM Usuario u WHERE u.idUsuario = :id ")
    List<Curso> findCursosUsuarioForId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.nombreUsuario = :newNombreUsuario WHERE u.id = :id")
    Optional<Integer> changeNombreUsuarioForId(@Param("id") Long id,
            @Param("newNombreUsuario") String newNombreUsuario);

    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u = :usuarioModificado WHERE u.id = :id")
    Optional<Integer> changeUsuarioForId(@Param("id") Long id, @Param("usuarioModificado") Usuario usuarioModificado);

    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.planUsuario = :newPlan WHERE u.idUsuario = :id")
    Optional<Integer> changePlanUsuarioForId(@Param("id") Long id, @Param("newPlan") Plan newPlan);

    @Query("SELECT u FROM Usuario u WHERE u.rollUsuario = RoleEnum.PROF OR u.rollUsuario = RoleEnum.ADMIN")
    List<Usuario> findProfes();

    @Query("SELECT u FROM Usuario u WHERE u.rollUsuario = RoleEnum.PROF OR u.rollUsuario = RoleEnum.ADMIN")
    List<Usuario> getInit();

    @Query("SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
