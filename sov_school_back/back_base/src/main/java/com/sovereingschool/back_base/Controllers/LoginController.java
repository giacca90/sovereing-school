package com.sovereingschool.back_base.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.ChangePassword;
import com.sovereingschool.back_base.Services.LoginService;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Utils.JwtUtil;

@RestController
@PreAuthorize("hasAnyRole('GUEST', 'USER', 'PROF', 'ADMIN')")
@RequestMapping("/login")
public class LoginController {

	@Autowired
	// TODO: Volver a activar la interfaz despues de mejorar el manejo de errores
	// private ILoginService loginService;
	private LoginService loginService;

	@Autowired
	private JwtUtil jwtUtil;

	@GetMapping("/{correo}")
	public ResponseEntity<?> conpruebaCorreo(@PathVariable String correo) {
		Object response = new Object();
		try {
			response = this.loginService.compruebaCorreo(correo);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener el correo del usuario: " + e.getMessage();
			return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/{id}/{password}")
	public ResponseEntity<?> getUsuario(@PathVariable Long id, @PathVariable String password) {
		Object response = new Object();
		try {
			AuthResponse authResponse = this.loginService.loginUser(id, password);
			if (authResponse == null || !authResponse.status()) {
				response = "Usuario no encontrado en getUsuario";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			String refreshToken = authResponse.refreshToken();
			authResponse = new AuthResponse(authResponse.status(), authResponse.message(), authResponse.usuario(),
					authResponse.accessToken(), null);
			response = authResponse;

			// Construir la cookie segura
			ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
					.httpOnly(true) // No accesible desde JavaScript
					.secure(true) // Solo por HTTPS
					.path("/") // Ruta donde será accesible
					.maxAge(15 * 24 * 60 * 60) // 15 días
					.sameSite("None") // Cambia a "None" si trabajas con frontend separado
					.build();

			return ResponseEntity.ok()
					.header("Set-Cookie", refreshTokenCookie.toString())
					.body(response);
		} catch (Exception e) {
			response = "Error de login: " + e.getMessage() + "\n" + e.getCause();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/correo/{id}")
	public ResponseEntity<?> getCorreoLogin(@PathVariable Long id) {
		Object response = new Object();
		try {
			String correo = this.loginService.getCorreoLogin(id);
			if (correo == null) {
				response = "Usuario no encontrado en getCorreoLogin";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			response = correo;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener el correo: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/password/{id}")
	public ResponseEntity<?> getPasswordLogin(@PathVariable Long id) {
		Object response = new Object();
		try {
			String password = this.loginService.getPasswordLogin(id);
			if (password == null) {
				response = "Usuario no encontrado en getPasswordLogin";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			response = password;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener la contraseña: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/new")
	public ResponseEntity<?> createNuevoLogin(@RequestBody Login login) {
		Object response = new Object();
		try {
			response = this.loginService.createNuevoLogin(login);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en crear el nuevo login " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/cambiaCorreo")
	public ResponseEntity<?> changeCorreoLogin(@RequestBody Login login) {
		Object response = new Object();
		try {
			if (login.getCorreo_electronico() == null) {
				response = "El correo electrónico no puede ser vació";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			if (login.getCorreo_electronico().length() < 1) {
				response = "El correo electrónico no puede ser vació";
				return new ResponseEntity<>(response, HttpStatus.FAILED_DEPENDENCY);
			}
			response = this.loginService.changeCorreoLogin(login);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en cambiar de correo: " + e.getMessage();
			return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/cambiaPassword")
	public ResponseEntity<?> changePasswordLogin(@RequestBody ChangePassword changepassword) {
		Object response = new Object();
		try {
			Integer respuesta = this.loginService.changePasswordLogin(changepassword);
			if (respuesta == null) {
				response = "La contraseña no puede estar vacía";
				return new ResponseEntity<>(response, HttpStatus.FAILED_DEPENDENCY);
			}
			if (respuesta == 0) {
				response = "Las contraseñas no coinciden";
				return new ResponseEntity<>(response, HttpStatus.FAILED_DEPENDENCY);
			}
			response = "Contraseña cambiada con éxito!!!";
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en cambiar la contraseña: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteLogin(@PathVariable Long id) {
		Object response = new Object();
		try {
			response = this.loginService.deleteLogin(id);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en cambiar la contraseña: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refreshAccessToken(
			@CookieValue(required = false) String refreshToken) {
		Object response = new Object();
		try {
			if (refreshToken == null || refreshToken.isEmpty()) {
				response = "El refresh-token no puede ser vacio";
				return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
			}

			Long idUsuario = this.jwtUtil.getIdUsuario(refreshToken);

			AuthResponse authResponse = this.loginService.refreshAccessToken(idUsuario);
			if (authResponse == null || !authResponse.status()) {
				response = "Usuario no encontrado en refreshAccessToken";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			String newRefreshToken = authResponse.refreshToken();

			response = new AuthResponse(authResponse.status(), authResponse.message(), authResponse.usuario(),
					authResponse.accessToken(), null);

			// Construir la cookie segura
			ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", newRefreshToken)
					.httpOnly(true) // No accesible desde JavaScript
					.secure(true) // Solo por HTTPS
					.path("/") // Ruta donde será accesible
					.maxAge(15 * 24 * 60 * 60) // 15 días
					.sameSite("None") // Cambia a "None" si trabajas con frontend separado
					.build();

			return ResponseEntity.ok()
					.header("Set-Cookie", refreshTokenCookie.toString())
					.body(response);
		} catch (Exception e) {
			response = "Error en refrescar el AccessToken: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/loginWithToken")
	public ResponseEntity<?> loginWithToken(@RequestBody String token) {
		Object response = new Object();
		if (token == null || token.isEmpty()) {
			response = "Token no puede ser vacio";
			return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
		}
		try {
			response = this.loginService.loginWithToken(token);
			Usuario usuario = (Usuario) response;
			Authentication auth = jwtUtil.createAuthenticationFromToken(token);
			String refreshToken = jwtUtil.generateToken(auth, "refresh", usuario.getId_usuario());
			ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
					.httpOnly(true) // No accesible desde JavaScript
					.secure(true) // Solo por HTTPS
					.path("/") // Ruta donde será accesible
					.maxAge(15 * 24 * 60 * 60) // 15 días
					.sameSite("None") // Cambia a "None" si trabajas con frontend separado
					.build();
			return ResponseEntity.ok()
					.header("Set-Cookie", refreshTokenCookie.toString())
					.body(response);
		} catch (JWTVerificationException e) {
			System.err.println(e.getMessage());
			response = e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
		} catch (Exception e) {
			System.err.println("Error en login con token: " + e.getMessage());
			response = "Error en login con token: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/logout")
	public ResponseEntity<?> logout() {
		try {
			// Construir la cookie segura
			ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
					.httpOnly(true) // No accesible desde JavaScript
					.secure(true) // Solo por HTTPS
					.path("/") // Ruta donde será accesible
					.maxAge(0) // 0 días
					.sameSite("None") // Cambia a "None" si trabajas con frontend separado
					.build();

			return ResponseEntity.ok()
					.header("Set-Cookie", refreshTokenCookie.toString())
					.body("Logout exitoso");
		} catch (Exception e) {
			return new ResponseEntity<>("Error en logout: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
