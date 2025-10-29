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

    @Query("SELECT l.idUsuario FROM Login l WHERE l.correoElectronico = :correo")
    Optional<Long> compruebaCorreo(@Param("correo") String correo);

    @Query("SELECT l.correoElectronico FROM Login l WHERE l.idUsuario = :id")
    Optional<String> findCorreoLoginForId(@Param("id") Long id);

    @Query("SELECT l.password FROM Login l WHERE l.idUsuario = :id")
    Optional<String> findPasswordLoginForId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Login l SET l.correoElectronico = :newCorreo WHERE l.idUsuario = :id")
    Optional<Integer> changeCorreoLoginForId(@Param("id") Long id, @Param("newCorreo") String newCorreo);

    @Modifying
    @Transactional
    @Query("UPDATE Login l SET l.password = :newPassword WHERE l.idUsuario = :id")
    Optional<Integer> changePasswordLoginForId(@Param("id") Long id, @Param("newPassword") String newPassword);

    @Query("SELECT l FROM Login l WHERE l.correoElectronico = :correo")
    Optional<Login> getLoginForCorreo(@Param("correo") String correo);
}
