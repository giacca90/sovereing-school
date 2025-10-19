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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_usuario;

    @Column(nullable = false)
    private String nombre_usuario;

    @Column(unique = true, columnDefinition = "text[]")
    private List<String> foto_usuario;

    @Column(length = 1500)
    private String presentacion;

    @Column(nullable = false, name = "roll_usuario")
    @Enumerated(EnumType.STRING) // Esto es importante
    private RoleEnum roll_usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_usuario", referencedColumnName = "id_plan")
    private Plan plan_usuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_curso", joinColumns = @JoinColumn(name = "id_usuario"), inverseJoinColumns = @JoinColumn(name = "id_curso"))
    @JsonIgnoreProperties({ "clases_curso", "planes_curso", "precio_curso" })
    private List<Curso> cursos_usuario;

    @Temporal(TemporalType.DATE)
    private Date fecha_registro_usuario;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "account_no_expired")
    private Boolean accountNoExpired;

    @Column(name = "account_no_locked")
    private Boolean accountNoLocked;

    @Column(name = "credentials_no_expired")
    private Boolean credentialsNoExpired;
}