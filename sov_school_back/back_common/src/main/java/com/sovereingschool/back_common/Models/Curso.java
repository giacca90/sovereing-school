package com.sovereingschool.back_common.Models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "curso")
public class Curso implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_curso")
	private Long idCurso;

	@Column(name = "nombre_curso", unique = true, nullable = false)
	private String nombreCurso;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "curso_profesor", joinColumns = @JoinColumn(name = "id_curso"), inverseJoinColumns = @JoinColumn(name = "id_usuario"))
	@JsonIgnoreProperties({ "rollUsuario", "planUsuario", "cursosUsuario", "fechaRegistroUsuario" })
	private List<Usuario> profesoresCurso;

	private Date fechaPublicacionCurso;

	@OneToMany(mappedBy = "cursoClase", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	@JsonIgnoreProperties({ "cursoClase" }) // evita recursión infinita
	private List<Clase> clasesCurso;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "cursos_plan", joinColumns = @JoinColumn(name = "id_curso"), inverseJoinColumns = @JoinColumn(name = "id_plan"))
	@JsonIgnoreProperties({ "precioPlan", "cursosPlan" })
	private List<Plan> planesCurso;

	private String descripcionCorta;

	@Column(length = 1500)
	private String descripcionLarga;

	private String imagenCurso;

	private BigDecimal precioCurso;

}
