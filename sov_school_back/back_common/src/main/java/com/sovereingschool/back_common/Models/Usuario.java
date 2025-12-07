package com.sovereingschool.back_common.Models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de Usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre_usuario", nullable = false)
    private String nombreUsuario;

    @Column(name = "foto_usuario", columnDefinition = "text[]")
    private List<String> fotoUsuario;

    @Column(length = 1500)
    private String presentacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "roll_usuario", nullable = false)
    private RoleEnum rollUsuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_usuario", referencedColumnName = "id_plan")
    private Plan planUsuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_curso", joinColumns = @JoinColumn(name = "id_usuario"), inverseJoinColumns = @JoinColumn(name = "id_curso"))
    @JsonIgnoreProperties({ "clasesCurso", "planesCurso", "precioCurso" })
    private List<Curso> cursosUsuario;

    @Column(name = "fecha_registro_usuario")
    private Date fechaRegistroUsuario;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "account_no_expired")
    private Boolean accountNoExpired;

    @Column(name = "account_no_locked")
    private Boolean accountNoLocked;

    @Column(name = "credentials_no_expired")
    private Boolean credentialsNoExpired;
}
