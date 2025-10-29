package com.sovereingschool.back_common.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_common.Models.Clase;

import jakarta.transaction.Transactional;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Long> {

	@Modifying
	@Transactional
	@Query("UPDATE Clase cl SET cl.nombreClase = :nombreClase, cl.tipoClase = :tipoClase, cl.direccionClase = :direccionClase, cl.posicionClase = :posicionClase WHERE cl.idClase = :idClase")
	void updateClase(@Param("idClase") Long idClase, @Param("nombreClase") String nombreClase,
			@Param("tipoClase") int tipoClase, @Param("direccionClase") String direccionClase,
			@Param("posicionClase") Integer posicionClase);

	@Query("SELECT c.nombreClase FROM Clase c WHERE c.idClase = :id")
	Optional<String> findNombreClaseById(@Param("id") Long id);

	@Query("SELECT c FROM Clase c WHERE c.direccionClase = :direccionClase")
	Optional<Clase> findByDireccionClase(@Param("direccionClase") String direccionClase);
}
