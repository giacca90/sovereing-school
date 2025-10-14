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
import lombok.ToString;

/* @AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter */
@Data
@ToString
@Entity
@Table(name = "clase")
public class Clase implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_clase;

    @Column(nullable = false)
    private String nombre_clase;

    @Column(length = 1000)
    private String descriccion_clase;

    @Column(length = 10000)
    private String contenido_clase;

    // 0 - ESTATICO, 1 - OBS - 2 - WEBCAM
    @Column(nullable = false)
    private int tipo_clase;

    @Column()
    private String direccion_clase;

    @Column(nullable = false)
    private Integer posicion_clase;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    @JsonBackReference
    private Curso curso_clase;

    @Override
    public int hashCode() {
        return Objects.hash(id_clase, direccion_clase, contenido_clase, posicion_clase, nombre_clase);
    }

    @Override
    public boolean equals(Object o) {
        Clase clase = (Clase) o;
        return this.id_clase == clase.id_clase && this.nombre_clase == clase.nombre_clase
                && this.descriccion_clase == clase.descriccion_clase && this.contenido_clase == clase.contenido_clase
                && this.direccion_clase == clase.direccion_clase;
    }

}