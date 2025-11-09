package com.sovereingschool.back_base.DTOs;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "idUsuario", "oldPassword", "newPassword" })
public record ChangePassword(
		Long idUsuario,
		String oldPassword,
		String newPassword) implements Serializable {
}
