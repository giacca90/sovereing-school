package com.sovereingschool.back_common.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sovereingschool.back_common.DTOs.ClaseDTO;
import com.sovereingschool.back_common.DTOs.CursoDTO;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.PlanRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

@Component
public class CursoUtil {

        private UsuarioRepository usuarioRepo;
        private PlanRepository planRepo;

        private Logger logger = LoggerFactory.getLogger(CursoUtil.class);

        public CursoUtil(UsuarioRepository usuarioRepo, PlanRepository planRepo) {
                this.usuarioRepo = usuarioRepo;
                this.planRepo = planRepo;
        }

        public Curso cursoDTOToCurso(CursoDTO cursoDTO) throws NotFoundException {

                // Profesores → lista mutable
                List<Usuario> profesores = new ArrayList<>();
                for (Long idProfesor : cursoDTO.profesoresCurso()) {
                        profesores.add(
                                        this.usuarioRepo.findById(idProfesor)
                                                        .orElseThrow(() -> {
                                                                logger.error("Error al obtener el profesor con ID {}",
                                                                                idProfesor);
                                                                return new NotFoundException(
                                                                                "Error al obtener el profesor con ID "
                                                                                                + idProfesor);
                                                        }));
                }

                // Clases → ¡importantísimo! convertir a lista MUTABLE
                List<Clase> clases = cursoDTO.clasesCurso().stream()
                                .map(this::claseDTOToClase)
                                .collect(Collectors.toCollection(ArrayList::new)); // 🔥 LISTA MUTABLE 🔥

                // Planes → lista mutable también
                List<Plan> planes = cursoDTO.planesCurso().stream()
                                .map(idPlan -> this.planRepo.findById(idPlan).orElse(null))
                                .collect(Collectors.toCollection(ArrayList::new));

                return new Curso(
                                cursoDTO.idCurso(),
                                cursoDTO.nombreCurso(),
                                profesores,
                                cursoDTO.fechaPublicacionCurso(),
                                clases,
                                planes,
                                cursoDTO.descripcionCorta(),
                                cursoDTO.descripcionLarga(),
                                cursoDTO.imagenCurso(),
                                cursoDTO.precioCurso());
        }

        public Clase claseDTOToClase(ClaseDTO claseDTO) {
                Clase clase = new Clase();
                clase.setIdClase(claseDTO.idClase());
                clase.setNombreClase(claseDTO.nombreClase());
                clase.setDescripcionClase(claseDTO.descripcionClase());
                clase.setContenidoClase(claseDTO.contenidoClase());
                clase.setTipoClase(claseDTO.tipoClase());
                clase.setDireccionClase(claseDTO.direccionClase());
                clase.setPosicionClase(claseDTO.posicionClase());

                // El curso se asigna LUEGO en creaClasesCurso()
                clase.setCursoClase(null);

                return clase;
        }

        public CursoDTO cursoToCursoDTO(Curso curso) {
                return new CursoDTO(
                                curso.getIdCurso(),
                                curso.getNombreCurso(),
                                curso.getProfesoresCurso() != null
                                                ? curso.getProfesoresCurso().stream().map(Usuario::getIdUsuario)
                                                                .toList()
                                                : List.of(),
                                curso.getFechaPublicacionCurso(),
                                curso.getClasesCurso() != null
                                                ? curso.getClasesCurso().stream().map(this::claseToClaseDTO).toList()
                                                : List.of(),
                                curso.getPlanesCurso() != null
                                                ? curso.getPlanesCurso().stream().map(Plan::getIdPlan).toList()
                                                : List.of(),
                                curso.getDescripcionCorta(),
                                curso.getDescripcionLarga(),
                                curso.getImagenCurso(),
                                curso.getPrecioCurso());
        }

        public ClaseDTO claseToClaseDTO(Clase clase) {
                return new ClaseDTO(
                                clase.getIdClase(),
                                clase.getNombreClase(),
                                clase.getDescripcionClase(),
                                clase.getContenidoClase(),
                                clase.getTipoClase(),
                                clase.getDireccionClase(),
                                clase.getPosicionClase(),
                                clase.getCursoClase() != null ? clase.getCursoClase().getIdCurso() : null); // Check
                                                                                                            // para null
        }
}
