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
    private Long idClase;

    @Column(nullable = false)
    private String nombreClase;

    @Column(length = 1000)
    private String descriccionClase;

    @Column(length = 10000)
    private String contenidoClase;

    // 0 - ESTATICO, 1 - OBS - 2 - WEBCAM
    @Column(nullable = false)
    private int tipoClase;

    @Column()
    private String direccionClase;

    @Column(nullable = false)
    private Integer posicionClase;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    @JsonBackReference
    private Curso cursoClase;

    @Override
    public int hashCode() {
        return Objects.hash(idClase, direccionClase, contenidoClase, posicionClase, nombreClase);
    }

    @Override
    public boolean equals(Object o) {
        Clase clase = (Clase) o;
        return this.idClase == clase.idClase &&
                this.nombreClase == clase.nombreClase &&
                this.descriccionClase == clase.descriccionClase &&
                this.contenidoClase == clase.contenidoClase &&
                this.direccionClase == clase.direccionClase;
    }

}