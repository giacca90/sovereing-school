package com.sovereingschool.back_base.Controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sovereingschool.back_base.Interfaces.ICursoService;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/cursos")
@PreAuthorize("hasAnyRole('GUEST', 'USER', 'PROF', 'ADMIN')")
public class CursoController {
	private ICursoService cursoService;

	public CursoController(ICursoService cursoService) {
		this.cursoService = cursoService;
	}

	/* Parte de gestión de cursos */
	@GetMapping("/getAll")
	public ResponseEntity<?> getAll() {
		Object response = new Object();
		try {
			response = this.cursoService.getAll();
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener los cursos: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getCurso/{id}")
	public ResponseEntity<?> getCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			Curso curso = this.cursoService.getCurso(id);
			response = curso;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en obtener el curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getNombreCurso/{id}")
	public ResponseEntity<?> getNombreCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			String nombreCurso = this.cursoService.getNombreCurso(id);
			response = nombreCurso;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en obtener el nombre del curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getNombresProfesoresCurso/{id}")
	public ResponseEntity<?> getNombresProfesoresCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			List<Usuario> profesores = this.cursoService.getProfesoresCurso(id);
			List<String> nombresProfesores = profesores.stream()
					.map(Usuario::getNombreUsuario)
					.toList();

			response = nombresProfesores;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en obtener los nombres de los profesores: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getFechaCreacionCurso/{id}")
	public ResponseEntity<?> getFechaCreacionCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			Date fecha = this.cursoService.getFechaCreacionCurso(id);
			response = fecha;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en obtener la fecha del curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getClasesDelCurso/{id}")
	public ResponseEntity<?> getClasesDelCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			List<Clase> clases = this.cursoService.getClasesDelCurso(id);
			if (clases.isEmpty()) {
				response = "Curso no encontrado";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			response = clases;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener las clases del curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getPlanesDelCurso/{id}")
	public ResponseEntity<?> getPlanesDelCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			List<Plan> planes = this.cursoService.getPlanesDelCurso(id);
			if (planes.isEmpty()) {
				response = "Curso no encontrado";
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			response = planes;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response = "Error en obtener los planes del curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/getPrecioCurso/{id}")
	public ResponseEntity<?> getPrecioCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			BigDecimal precio = this.cursoService.getPrecioCurso(id);
			response = precio;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en obtener el precio del curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('PROF', 'ADMIN')")
	@PutMapping("/update")
	public ResponseEntity<?> updateCurso(@RequestBody Curso curso) {
		Object response = new Object();
		try {
			response = this.cursoService.updateCurso(curso);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (NotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (InternalComunicationException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.FAILED_DEPENDENCY);
		} catch (InternalServerException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RepositoryException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_MODIFIED);
		}
	}

	@PreAuthorize("hasAnyRole('PROF', 'ADMIN')")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteCurso(@PathVariable Long id) {
		Object response = new Object();
		try {
			this.cursoService.deleteCurso(id);
			response = "Curso eliminado con éxito!!!";
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en eliminar el curso: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/* Parte de gestión de clases */
	@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
	@GetMapping("/{idCurso}/getClaseForId/{idClase}")
	public ResponseEntity<?> getClaseForId(@PathVariable Long idCurso, @PathVariable Long idClase) {
		try {
			Curso curso = this.cursoService.getCurso(idCurso);
			List<Clase> clases = curso.getClasesCurso();

			// Buscar la clase usando stream
			Optional<Clase> claseOpt = clases.stream()
					.filter(c -> c.getIdClase().equals(idClase))
					.findFirst();

			return claseOpt
					.<ResponseEntity<?>>map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("Clase no encontrada"));

		} catch (EntityNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
		} catch (Exception e) {
			String mensaje = "Error al encontrar la clase: " + e.getMessage();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensaje);
		}
	}

	@PreAuthorize("hasAnyRole('PROF', 'ADMIN')")
	@DeleteMapping("/{idCurso}/deleteClase/{idClase}")
	public ResponseEntity<?> deleteClase(@PathVariable Long idCurso, @PathVariable Long idClase) {
		Object response = new Object();
		try {
			Curso curso = this.cursoService.getCurso(idCurso);
			List<Clase> clases = curso.getClasesCurso();
			Clase eliminada = null;
			for (int i = 0; i < clases.size(); i++) {
				if (eliminada != null) {
					clases.get(i).setPosicionClase(clases.get(i).getPosicionClase() - 1);
				} else {
					if (clases.get(i).getIdClase().equals(idClase)) {
						eliminada = clases.get(i);
					}
				}
			}
			if (eliminada != null) {
				clases.remove(eliminada);
				curso.setClasesCurso(clases);
				this.cursoService.updateCurso(curso);
				this.cursoService.deleteClase(eliminada);
				response = "Clase eliminada con exito!!!";
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			response = "Clase no encontrada";
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		} catch (EntityNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			response = "Error en borrar la clase: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('PROF', 'ADMIN')")
	@PostMapping("/subeVideo/{idCurso}/{idClase}")
	public ResponseEntity<?> subeVideo(@PathVariable Long idCurso, @PathVariable Long idClase,
			@RequestParam("video") MultipartFile file) {
		Object response = new Object();
		try {
			if (file.isEmpty()) {
				response = "Archivo vacío";
				return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
			}
			String filePath = this.cursoService.subeVideo(file);
			response = filePath;
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (AccessDeniedException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException | IllegalStateException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			response = "Error en subir el video: " + e.getMessage();
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
