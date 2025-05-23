package com.sovereingschool.back_common.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_common.Models.Login;

import jakarta.transaction.Transactional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {

    @Query("SELECT l.id_usuario FROM Login l WHERE l.correo_electronico = :correo")
    Optional<Long> compruebaCorreo(@Param("correo") String correo);

    @Query("SELECT l.correo_electronico FROM Login l WHERE l.id_usuario = :id")
    Optional<String> findCorreoLoginForId(@Param("id") Long id);

    @Query("SELECT l.password FROM Login l WHERE l.id_usuario = :id")
    Optional<String> findPasswordLoginForId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Login l SET l.correo_electronico = :new_correo WHERE l.id_usuario = :id")
    Optional<Integer> changeCorreoLoginForId(@Param("id") Long id, @Param("new_correo") String new_correo);

    @Modifying
    @Transactional
    @Query("UPDATE Login l SET l.password = :new_password WHERE l.id_usuario = :id")
    Optional<Integer> changePasswordLoginForId(@Param("id") Long id, @Param("new_password") String new_password);

    @Query("SELECT l FROM Login l WHERE l.correo_electronico = :correo")
    Optional<Login> getLoginForCorreo(@Param("correo") String correo);
}
