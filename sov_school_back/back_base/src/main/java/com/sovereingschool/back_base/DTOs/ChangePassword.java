package com.sovereingschool.back_base.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePassword {
	private Long idUsuario;
	private String oldPassword;
	private String newPassword;
}
