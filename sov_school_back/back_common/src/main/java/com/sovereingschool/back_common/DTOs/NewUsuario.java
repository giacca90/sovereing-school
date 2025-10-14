package com.sovereingschool.back_common.DTOs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/* @AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter */
@Data
@AllArgsConstructor
@ToString
public class NewUsuario implements Serializable {
	private String nombre_usuario;

	private String correo_electronico;

	private String password;

	private List<String> foto_usuario;

	private Plan plan_usuario;

	private List<Curso> cursos_usuario;

	private Date fecha_registro_usuario;
}
