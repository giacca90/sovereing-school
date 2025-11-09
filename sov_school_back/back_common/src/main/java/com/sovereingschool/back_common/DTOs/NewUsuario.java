package com.sovereingschool.back_common.DTOs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;

@JsonPropertyOrder({ "nombreUsuario", "correoElectronico", "password", "fotoUsuario", "planUsuario", "cursosUsuario",
		"fechaRegistroUsuario" })
public record NewUsuario(
		String nombreUsuario,
		String correoElectronico,
		String password,
		List<String> fotoUsuario,
		Plan planUsuario,
		List<Curso> cursosUsuario,
		Date fechaRegistroUsuario) implements Serializable {
}
