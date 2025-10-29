package com.sovereingschool.back_base.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChangePassword {
	private Long idUsuario;
	private String oldPassword;
	private String newPassword;
}
