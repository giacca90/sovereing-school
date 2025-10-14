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
import lombok.Data;

/* @AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString */
@Data
@Entity
@Table(name = "curso")
public class Curso implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id_curso;

	@Column(unique = true, nullable = false)
	private String nombre_curso;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "curso_profesor", joinColumns = @JoinColumn(name = "id_curso"), inverseJoinColumns = @JoinColumn(name = "id_usuario"))
	@JsonIgnoreProperties({ "roll_usuario", "plan_usuario", "cursos_usuario",
			"fecha_registro_usuario" })
	private List<Usuario> profesores_curso;

	private Date fecha_publicacion_curso;

	@OneToMany(mappedBy = "curso_clase", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnoreProperties({ "curso_clase" })
	@JsonManagedReference
	private List<Clase> clases_curso;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "cursos_plan", joinColumns = @JoinColumn(name = "id_curso"), inverseJoinColumns = @JoinColumn(name = "id_plan"))
	@JsonIgnoreProperties({ "precio_plan", "cursos_plan" })
	private List<Plan> planes_curso;

	private String descriccion_corta;

	@Column(length = 1500)
	private String descriccion_larga;

	private String imagen_curso;

	private BigDecimal precio_curso;

}