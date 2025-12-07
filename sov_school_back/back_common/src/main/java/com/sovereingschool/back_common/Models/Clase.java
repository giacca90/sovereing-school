package com.sovereingschool.back_common.Models;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "clase")
public class Clase implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clase")
    private Long idClase;

    @Column(name = "nombre_clase", nullable = false)
    private String nombreClase;

    @Column(name = "descripcion_clase", length = 1000)
    private String descripcionClase;

    @Column(name = "contenido_clase", length = 10000)
    private String contenidoClase;

    // 0 - ESTATICO, 1 - OBS - 2 - WEBCAM
    @Column(name = "tipo_clase", nullable = false)
    private int tipoClase;

    @Column(name = "direccion_clase")
    private String direccionClase;

    @Column(name = "posicion_clase", nullable = false)
    private Integer posicionClase;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    @JsonBackReference
    @ToString.Exclude // ✅ evita recursión infinita en el toString()
    private Curso cursoClase;

    // ✅ Usa equals() y hashCode() basados solo en el ID (recomendado por Hibernate)
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Clase))
            return false;
        Clase clase = (Clase) o;
        return Objects.equals(idClase, clase.idClase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idClase);
    }
}
