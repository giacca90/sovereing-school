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
	@Query("UPDATE Clase cl SET cl.nombre_clase = :nombre_clase, cl.tipo_clase = :tipo_clase, cl.direccion_clase = :direccion_clase, cl.posicion_clase = :posicion_clase WHERE cl.id_clase = :id_clase")
	void updateClase(@Param("id_clase") Long id_clase, @Param("nombre_clase") String nombre_clase,
			@Param("tipo_clase") int tipo_clase, @Param("direccion_clase") String direccion_clase,
			@Param("posicion_clase") Integer posicion_clase);

	@Query("SELECT c.nombre_clase FROM Clase c WHERE c.id_clase = :id")
	Optional<String> findNombreClaseById(@Param("id") Long id);

	@Query("SELECT c FROM Clase c WHERE c.direccion_clase = :direccion_clase")
	Optional<Clase> findByDireccionClase(@Param("direccion_clase") String direccion_clase);
}
