package com.sovereingschool.back_common.DTOs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class NewUsuario implements Serializable {
	private String nombreUsuario;

	private String correoElectronico;

	private String password;

	private List<String> fotoUsuario;

	private Plan planUsuario;

	private List<Curso> cursosUsuario;

	private Date fechaRegistroUsuario;
}
