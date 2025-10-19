package com.sovereingschool.back_base.Controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.CursosUsuario;
import com.sovereingschool.back_base.Interfaces.IUsuarioService;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.persistence.EntityNotFoundException;

@RestController
@PreAuthorize("hasAnyRole('GUEST', 'USER', 'PROF', 'ADMIN')")
@RequestMapping("/usuario")
public class UsuarioController {

	public static Object convertirBase64AObjeto(String base64) throws IOException, ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(base64);
		ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(data));
		return objStream.readObject();
	}

	private IUsuarioService usuarioService;
	private JwtUtil jwtUtil;

	@Value("${variable.FOTOS_DIR}")
	private String uploadDir;

	@Value("${variable.BACK_BASE}")
	private String back_base;

	public UsuarioController(IUsuarioService usuarioService, JwtUtil jwtUtil) {
		this.usuarioService = usuarioService;
		this.jwtUtil = jwtUtil;
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getUsuario(@PathVariable Long id) {
		Object response = new Object();
		try {
			Usuario usuario = this.usuarioService.getUsuario(id);
			response = usuario;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en buscar el usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/nombre/{id}")
	public ResponseEntity<?> getNombreUsuario(@PathVariable Long id) {
		Object response = new Object();
		try {
			String nombre = this.usuarioService.getNombreUsuario(id);
			response = nombre;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en encontrar el nombre del usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/fotos/{nombreFoto}")
	public ResponseEntity<?> getFotos(@PathVariable String nombreFoto) {
		Object response = new Object();

		Path photoPath = Paths.get(uploadDir).resolve(nombreFoto).normalize();
		File photoFile = photoPath.toFile();

		if (!photoFile.exists() || !photoFile.isFile()) {
			response = "Foto no encontrada.";
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}

		// Construir el recurso a partir del archivo
		FileSystemResource resource = new FileSystemResource(photoFile);

		// Determinar el tipo de contenido
		String contentType;
		try {
			contentType = Files.probeContentType(photoPath);
		} catch (IOException e) {
			response = "Error determinando el tipo de contenido." + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Configurar cache control
		CacheControl cacheControl = CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic();

		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(cacheControl.getHeaderValue());
		headers.set("Pragma", "cache");
		headers.set("Expires", ZonedDateTime.now().plusDays(30).format(DateTimeFormatter.RFC_1123_DATE_TIME));
		headers.setContentType(
				contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.inline().filename(resource.getFilename()).build());
		response = resource;
		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	@GetMapping("/roll/{id}")
	public ResponseEntity<?> getRollUsuario(@PathVariable Long id) {
		Object response = new Object();
		try {
			RoleEnum roll = this.usuarioService.getRollUsuario(id);
			response = roll;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en obtener el roll del usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/plan/{id}")
	public ResponseEntity<?> getPlanUsuario(@PathVariable Long id) {
		Object response = new Object();
		try {
			Plan plan = this.usuarioService.getPlanUsuario(id);
			response = plan;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en obtener el plan del usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/cursos/{id}")
	public ResponseEntity<?> getCursosUsuario(@PathVariable Long id) {
		Object response = new Object();
		try {
			List<Curso> cursos = this.usuarioService.getCursosUsuario(id);
			response = cursos;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>("No existe el usuario", HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en obtener los cursos del usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/nuevo")
	public ResponseEntity<?> createUsuario(@RequestBody NewUsuario newUsuario) {
		Object response = new Object();
		try {
			this.usuarioService.sendConfirmationEmail(newUsuario);
			String resp = "Correo enviado con éxito!!!";
			response = resp;
			return ResponseEntity.ok()
					.body(response);
		} catch (AccessDeniedException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception ex) {
			response = "Error en crear el nuevo usuario: " + ex.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/confirmation")
	public ResponseEntity<?> confirmationEmail(@RequestBody String token) {
		Object response = new Object();
		try {
			this.jwtUtil.isTokenValid(token);
			String newUsuarioB64 = this.jwtUtil.getSpecificClaim(token, "new_usuario");
			if (newUsuarioB64 == null || newUsuarioB64.isEmpty()) {
				response = "Error en el token de acceso";
				return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
			}
			NewUsuario newUsuario = (NewUsuario) convertirBase64AObjeto(newUsuarioB64);

			AuthResponse authResponse = this.usuarioService.createUsuario(newUsuario);
			String refreshToken = authResponse.refreshToken();
			authResponse = new AuthResponse(authResponse.status(), authResponse.message(), authResponse.usuario(),
					authResponse.accessToken(), null);
			response = authResponse;

			// Construir la cookie segura
			ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofDays(15))
					.sameSite("None")
					.build();
			return ResponseEntity.ok()
					.header("Set-Cookie", refreshTokenCookie.toString())
					.body(response);
		} catch (AccessDeniedException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>("El usuario ya existe", HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			return new ResponseEntity<>("Error en crear el usuario: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
	@PutMapping("/edit")
	public ResponseEntity<?> editUsuario(@RequestBody Usuario usuario) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return new ResponseEntity<>("Error en el token de acceso: nulo o no autenticado", HttpStatus.UNAUTHORIZED);
		}
		Long idUsuario = (Long) authentication.getDetails();

		if (idUsuario == null || !idUsuario.equals(usuario.getId_usuario())) {
			return new ResponseEntity<>("Error en el token de acceso: idUsuario no coincide\nidUsuario: "
					+ usuario.getId_usuario() + "\nidUsuario del token: " + idUsuario, HttpStatus.UNAUTHORIZED);
		}
		Object response = new Object();
		try {
			this.usuarioService.updateUsuario(usuario).getId_usuario();
			response = "Usuario editado con éxito!!!";
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en editar el usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
	@PutMapping("/plan")
	public ResponseEntity<?> changePlanUsuario(@RequestBody Usuario usuario) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return new ResponseEntity<>("Error en el token de acceso", HttpStatus.UNAUTHORIZED);
		}
		Long idUsuario = (Long) authentication.getDetails();
		if (idUsuario == null || !idUsuario.equals(usuario.getId_usuario())) {
			return new ResponseEntity<>("Error en el token de acceso: no hay un id_usuario en el token",
					HttpStatus.UNAUTHORIZED);
		}
		Object response = new Object();
		try {
			this.usuarioService.changePlanUsuario(usuario);
			response = "Plan cambiado con éxito!!!";
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en cambiar el plan del usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasRole('USER') or hasRole('PROF') or hasRole('ADMIN')")
	@PutMapping("/cursos")
	public ResponseEntity<?> changeCursosUsuario(@RequestBody CursosUsuario cursosUsuario) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return new ResponseEntity<>("Error en el token de acceso", HttpStatus.UNAUTHORIZED);
		}
		Long idUsuario = (Long) authentication.getDetails();
		if (idUsuario == null || !idUsuario.equals(cursosUsuario.getId_usuario())) {
			return new ResponseEntity<>("Error en el token de acceso: no hay id_usuario en el token",
					HttpStatus.UNAUTHORIZED);
		}
		Object response = new Object();

		try {
			this.usuarioService.changeCursosUsuario(cursosUsuario);
			response = "Cursos actualizados con éxito!!!";
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en actualizar los cursos del usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteUsuario(@PathVariable Long id) {
		Object response = new Object();
		try {
			String result = this.usuarioService.deleteUsuario(id);
			response = result;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en eliminar el usuario: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/profes")
	public ResponseEntity<?> getProfes() {
		Object response = new Object();
		try {
			response = this.usuarioService.getProfes();
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener los profesores: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
	@PostMapping("/subeFotos")
	public ResponseEntity<?> uploadImages(@RequestBody MultipartFile[] files) {
		List<String> fileNames = new ArrayList<>();
		Object response = new Object();

		for (MultipartFile file : files) {
			// Obtener el nombre original del archivo o usar un valor predeterminado si es
			// null
			String originalFilename = file.getOriginalFilename();
			if (originalFilename == null) {
				originalFilename = "unknown_file";
			}

			// Genera un nombre único para cada archivo para evitar colisiones
			String fileName = UUID.randomUUID().toString() + "_"
					+ StringUtils.cleanPath(originalFilename).replace(" ", "_");
			Path filePath = Paths.get(uploadDir, fileName);
			if (!fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".svg")) {
				try {
					// Convertir la imagen a WebP
					BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
					BufferedImage webpImage = Scalr.resize(bufferedImage, Scalr.Method.QUALITY,
							bufferedImage.getWidth(),
							bufferedImage.getHeight());
					String webpFileName = fileName.replaceFirst("[.][^.]+$", "") + ".webp";
					Path webpFilePath = Paths.get(uploadDir, webpFileName);
					Files.createDirectories(webpFilePath.getParent());
					ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
					try (ImageOutputStream ios = ImageIO.createImageOutputStream(webpFilePath.toFile())) {
						writer.setOutput(ios);
						ImageWriteParam param = writer.getDefaultWriteParam();
						writer.write(null, new IIOImage(webpImage, null, null), param);
					}
					fileNames.add(back_base + "/usuario/fotos/" + webpFileName);
				} catch (IllegalArgumentException e) {
					return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
				} catch (EntityNotFoundException e) {
					return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
				} catch (RuntimeException e) {
					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				} catch (IOException e) {
					response = "Error en convertir la imagen: " + e.getMessage();
					return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				try {
					Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
					fileNames.add(back_base + "/usuario/fotos/" + fileName);
				} catch (IllegalArgumentException e) {
					return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
				} catch (EntityNotFoundException e) {
					return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
				} catch (RuntimeException e) {
					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				} catch (IOException e) {
					response = "Error en guardar la imagen: " + e.getMessage();
					return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		}
		response = fileNames;
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole('ADMIN')")
	@GetMapping("/getAll")
	public ResponseEntity<?> getAllUsuarios() {
		Object response = new Object();
		try {
			response = this.usuarioService.getAllUsuarios();
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (RuntimeException e) {
			response = e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en obtener todos los usuarios: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
