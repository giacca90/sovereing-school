package com.sovereingschool.back_base.Services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.Interfaces.ICursoService;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class CursoService implements ICursoService {

    private CursoRepository cursoRepo;
    private ClaseRepository claseRepo;
    private InitAppService initAppService;
    private WebClientConfig webClientConfig;

    private Logger logger = LoggerFactory.getLogger(CursoService.class);

    @Value("${variable.BACK_STREAM_DOCKER}")
    private String backStreamURL;

    @Value("${variable.BACK_CHAT_DOCKER}")
    private String backChatURL;

    private Path baseUploadDir;

    public CursoService(
            @Value("${variable.VIDEOS_DIR}") String uploadDir,
            CursoRepository cursoRepo,
            ClaseRepository claseRepo,
            InitAppService initAppService,
            WebClientConfig webClientConfig) {
        this.baseUploadDir = Paths.get(uploadDir);
        this.cursoRepo = cursoRepo;
        this.claseRepo = claseRepo;
        this.initAppService = initAppService;
        this.webClientConfig = webClientConfig;
    }

    @Override
    public Long createCurso(Curso newCurso) {
        newCurso.setIdCurso(null);
        Curso res = this.cursoRepo.save(newCurso);
        return res.getIdCurso();
    }

    /**
     * Función para obtener un curso
     * 
     * @param id_curso ID del curso
     * @return Curso con los datos del curso
     * @throws EntityNotFoundException si el curso no existe
     * 
     */
    @Override
    public Curso getCurso(Long idCurso) {
        return this.cursoRepo.findById(idCurso).orElseThrow(() -> {
            logger.error("CursoService: getCurso: Error en obtener el curso con ID {}: ", idCurso);
            return new EntityNotFoundException("Error en obtener el curso con ID " + idCurso);
        });
    }

    /**
     * Función para obtener el nombre del curso
     * 
     * @param id_curso ID del curso
     * @return String con el nombre del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    @Override
    public String getNombreCurso(Long idCurso) {
        return this.cursoRepo.findNombreCursoById(idCurso)
                .orElseThrow(() -> {
                    logger.error("Error en obtener el nombre del curso con ID {}", idCurso);
                    return new EntityNotFoundException("Error en obtener el nombre del curso con ID " + idCurso);
                });
    }

    /**
     * Función para obtener los profesores del curso
     * 
     * @param id_curso ID del curso
     * @return Lista de usuarios con los profesores del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    @Override
    public List<Usuario> getProfesoresCurso(Long idCurso) {
        List<Usuario> profesores = this.cursoRepo.findProfesoresCursoById(idCurso);
        if (profesores == null || profesores.isEmpty()) {
            logger.error("Error en obtener los profesores del curso con ID {}", idCurso);
            throw new EntityNotFoundException("Error en obtener los profesores del curso con ID " + idCurso);
        }
        return this.cursoRepo.findProfesoresCursoById(idCurso);
    }

    /**
     * Función para obtener la fecha de creación del curso
     * 
     * @param id_curso ID del curso
     * @return Date con la fecha de creación del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    @Override
    public Date getFechaCreacionCurso(Long idCurso) {
        return this.cursoRepo.findFechaCreacionCursoById(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener la fecha de creación del curso con ID {}", idCurso);
            return new EntityNotFoundException("Error en obtener la fecha de creación del curso con ID " + idCurso);
        });
    }

    @Override
    public List<Clase> getClasesDelCurso(Long idCurso) {
        return this.cursoRepo.findClasesCursoById(idCurso);
    }

    @Override
    public List<Plan> getPlanesDelCurso(Long idCurso) {
        return this.cursoRepo.findPlanesCursoById(idCurso);
    }

    /**
     * Función para obtener el precio del curso
     * 
     * @param id_curso ID del curso
     * @return BigDecimal con el precio del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    @Override
    public BigDecimal getPrecioCurso(Long idCurso) {
        return this.cursoRepo.findPrecioCursoById(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener el precio del curso con ID {}", idCurso);
            return new EntityNotFoundException("Error en obtener el precio del curso con ID " + idCurso);
        });
    }

    /**
     * 
     * Función para actualizar o crear un nuevo curso
     * 
     * @param curso Curso: curso a actualizar
     * @return Curso con los datos actualizados
     * @throws EntityNotFoundException  si el curso no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el curso no tiene un ID
     * @throws IllegalStateException    si el curso no tiene un ID
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     * 
     */
    @Override
    public Curso updateCurso(Curso curso) {
        List<Clase> clases = curso.getClasesCurso();
        curso.setClasesCurso(null);
        // Si el curso no existe, crear un nuevo
        if (curso.getIdCurso().equals(0L)) {
            curso.setIdCurso(null);
            curso = this.cursoRepo.save(curso);
        }
        // Crear la carpeta del curso si no existe
        Path cursoPath = baseUploadDir.resolve(curso.getIdCurso().toString());
        File cursoFile = new File(cursoPath.toString());
        if (!cursoFile.exists() || !cursoFile.isDirectory()) {
            if (!cursoFile.mkdir()) {
                logger.error("Error en crear la carpeta del curso.");
                throw new RuntimeException("Error en crear la carpeta del curso.");
            }
        }

        // Crear las clases del curso si no existen
        if (clases.isEmpty()) {
            for (Clase clase : clases) {
                clase.setCursoClase(curso);
                if (clase.getIdClase().equals(0L)) {
                    clase.setIdClase(null);
                }

                try {
                    clase = this.claseRepo.save(clase);
                } catch (Exception e) {
                    logger.error("Error en guardar la clase: {}", e.getMessage());
                    throw new RuntimeException("Error en guardar la clase: " + e.getMessage());
                }

                // Crea la carpeta de la clase si no existe
                Path clasePath = baseUploadDir.resolve(curso.getIdCurso().toString())
                        .resolve(clase.getIdClase().toString());
                File claseFile = new File(clasePath.toString());
                if (!claseFile.exists() || !claseFile.isDirectory()) {
                    if (!claseFile.mkdir()) {
                        throw new RuntimeException("Error en crear la carpeta de la clase");
                    }
                }
            }
            curso.setClasesCurso(clases);
            try {
                curso = this.cursoRepo.save(curso);
            } catch (Exception e) {
                logger.error("Error en actualizar el curso: {}", e.getMessage());
                throw new RuntimeException("Error en actualizar el curso: " + e.getMessage());
            }
        }

        // Actualizar el chat del curso
        try {
            WebClient webClient = webClientConfig.createSecureWebClient(backChatURL);
            webClient.post().uri("/actualizar_curso_chat")
                    .body(Mono.just(curso), Curso.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de chat: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de chat {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        if (res == null || !res.equals("Curso chat actualizado con éxito!!!")) {
                            logger.error("Error en actualizar el curso en el chat");
                            logger.error(res);
                            throw new RuntimeException("Error en actualizar el curso en el chat");
                        }
                    });
        } catch (Exception e) {
            logger.error("Error en actualizar el curso en el chat: {}", e.getMessage());
            throw new RuntimeException("Error en actualizar el curso en el chat: " + e.getMessage());
        }

        // Actializa el curso en el stream
        try {
            WebClient webClient = webClientConfig.createSecureWebClient(backStreamURL);
            webClient.post().uri("/actualizar_curso_stream")
                    .body(Mono.just(curso), Curso.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de streaming: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de streaming: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        if (res == null || !res.equals("Curso stream actualizado con éxito!!!")) {
                            logger.error("Error en actualizar el curso en el streaming:");
                            logger.error(res);
                            throw new RuntimeException("Error en actualizar el curso en el streaming");
                        }
                    });
        } catch (Exception e) {
            logger.error("Error en actualizar el curso en el streaming: {}", e.getMessage());
            throw new RuntimeException("Error en actualizar el curso en el streaming: " + e.getMessage());
        }

        // Actualizar el SSR
        try {
            this.initAppService.refreshSSR();
        } catch (Exception e) {
            logger.error("Error en actualizar el SSR: {}", e.getMessage());
            throw new RuntimeException("Error en actualizar el SSR: " + e.getMessage());
        }
        return curso;
    }

    /**
     * Función para eliminar un curso
     * 
     * @param id_curso ID del curso
     * @return Boolean con el resultado de la operación
     * @throws EntityNotFoundException si el curso no existe
     * @throws RuntimeException        si ocurre un error en el servidor
     * 
     */
    @Override
    public Boolean deleteCurso(Long idCurso) {
        this.cursoRepo.findById(idCurso).orElseThrow(() -> {
            logger.error("CursoService: deleteCurso: Error en obtener el curso con ID {}: ", idCurso);
            return new EntityNotFoundException("Error en obtener el curso con ID " + idCurso);
        });
        if (this.getCurso(idCurso).getClasesCurso() != null) {
            for (Clase clase : this.getCurso(idCurso).getClasesCurso()) {
                this.deleteClase(clase);
            }
        }
        this.cursoRepo.deleteById(idCurso);

        Path cursoPath = Paths.get(this.baseUploadDir.toString(), idCurso.toString());
        File cursoFile = new File(cursoPath.toString());
        if (cursoFile.exists()) {
            try {
                Files.walkFileTree(cursoPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                logger.error("Error al borrar la carpeta del curso: {}", e.getMessage());
                throw new RuntimeException("Error al borrar la carpeta del curso: " + e.getMessage());
            }
        } else {
            logger.error("La carpeta del curso no existe.");
        }

        // Eliminar el curso del microservicio de streaming
        try {
            WebClient webClient = webClientConfig.createSecureWebClient(backStreamURL);
            webClient.delete()
                    .uri("/deleteCurso/" + idCurso)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de stream: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(Boolean.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de streaming: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    })
                    .subscribe(res -> {
                        if (res == null || !res) {
                            logger.error("Error en borrar el curso en el servicio de reproducción");
                        }
                    });
        } catch (Exception e) {
            logger.error("Error al conectar con el microservicio de streaming: {}", e.getMessage());
            throw new RuntimeException("Error al conectar con el microservicio de streaming: " + e.getMessage());
        }

        // Eliminar el curso del microservicio de chat
        try {
            WebClient webClientChat = webClientConfig.createSecureWebClient(backChatURL);
            webClientChat.delete()
                    .uri("/delete_curso_chat/" + idCurso)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de chat: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de chat: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    })
                    .subscribe(res -> {
                        if (res == null || !res.equals("Curso chat borrado con exito!!!")) {
                            logger.error("Error al eliminar el curso del microservicio de chat");
                            logger.error(res);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error al conectar con el microservicio de chat: {}", e.getMessage());
            throw new RuntimeException("Error al conectar con el microservicio de chat: " + e.getMessage());
        }

        // Actualizar el SSR
        try {
            this.initAppService.refreshSSR();
        } catch (Exception e) {
            logger.error("Error en actualizar el SSR: {}", e.getMessage());
            throw new RuntimeException("Error en actualizar el SSR: " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<Curso> getAll() {
        return this.cursoRepo.findAll();
    }

    @Override
    public void deleteClase(Clase clase) {
        Optional<Clase> optionalClase = this.claseRepo.findById(clase.getIdClase());
        if (optionalClase.isPresent()) {
            this.claseRepo.delete(clase);
            if (clase.getDireccionClase().isEmpty()) {
                try {
                    Path path = Paths.get(clase.getDireccionClase()).getParent();
                    if (Files.exists(path) && !path.equals(baseUploadDir)) {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        logger.error("La carpeta de la clase no existe.");
                    }
                } catch (Exception e) {
                    logger.error("Error en borrar el video: {}", e.getMessage());
                }
            }

            // Eliminar la carpeta de la clase
            try {
                // Obtener token
                WebClient webClient = webClientConfig.createSecureWebClient(backStreamURL);
                webClient.delete()
                        .uri("/deleteClase/" + clase.getCursoClase().getIdCurso().toString() + "/"
                                + clase.getIdClase().toString())
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                    logger.error("Error HTTP del microservicio de stream: {}", errorBody);
                                    return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                                }))
                        .bodyToMono(Boolean.class)
                        .onErrorResume(e -> {
                            // Manejo de errores
                            logger.error("Error al conectar con el microservicio de streaming: {}", e.getMessage());
                            return Mono.empty(); // Continuar sin interrumpir la aplicación

                        }).subscribe(res -> {
                            // Maneja el resultado cuando esté disponible
                            if (res == null || !res) {
                                logger.error("Error en actualizar el curso en el servicio de reproducción");
                            }
                        });
            } catch (Exception e) {
                logger.error("Error en borrar la clase en el streaming: {}", e.getMessage());
            }

            // Elimina el chat de la clase
            try {
                WebClient webClient = webClientConfig.createSecureWebClient(backChatURL);
                webClient.delete()
                        .uri("/delete_clase_chat/" + clase.getCursoClase().getIdCurso().toString() + "/"
                                + clase.getIdClase().toString())
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                    logger.error("Error HTTP del microservicio de chat: {}", errorBody);
                                    return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                                }))
                        .bodyToMono(String.class)
                        .onErrorResume(e -> {
                            logger.error("Error al conectar con el microservicio de chat: {}", e.getMessage());
                            return Mono.empty(); // Continuar sin interrumpir la aplicación
                        }).subscribe(res -> {
                            if (res == null || !res.equals("Clase chat borrado con exito!!!")) {
                                logger.error("Error en borrar la clase en el chat");
                                logger.error(res);
                            }
                        });
            } catch (Exception e) {
                logger.error("Error en borrar la clase en el chat: {}", e.getMessage());
            }
            // TODO: Mirar si se necesita eliminar algo en el microservicio de streaming
            // en el microservicio de streaming hay que buscar todos los usuarios del curso,
            // y borrar la clase.

            // Actualizar el SSR
            try {
                this.initAppService.refreshSSR();
            } catch (Exception e) {
                logger.error("Error en actualizar el SSR: {}", e.getMessage());
                throw new RuntimeException("Error en actualizar el SSR: " + e.getMessage());
            }
        } else {
            logger.error("Clase no encontrada con ID: {}", clase.getIdClase());
        }
    }

    /**
     * Función para subir un video
     * 
     * @param file Archivo subido
     * @return String con la ruta del archivo subido
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     * @throws EntityNotFoundException  si el curso no existe
     * @throws IllegalArgumentException si el curso no tiene un ID
     * @throws IllegalStateException    si el curso no tiene un ID
     * @throws IOException              si ocurre un error en el servidor
     * @throws RuntimeException         si ocurre un error en el servidor
     */
    @Override
    public String subeVideo(MultipartFile file) {
        try {
            // Obtener el nombre original del archivo o usar un valor predeterminado si es
            // null
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) {
                originalFileName = "unknown_file";
            }

            // Define el path para guardar el archivo subido
            String cleanedFileName = StringUtils.cleanPath(originalFileName);
            Path filePath = baseUploadDir.resolve(UUID.randomUUID().toString() + "_" + cleanedFileName);

            // Guarda el archivo en el servidor
            Files.write(filePath, file.getBytes());
            return filePath.normalize().toString();
        } catch (AccessDeniedException e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new AccessDeniedException("Error en subir el video: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new IllegalArgumentException("Error en subir el video: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new EntityNotFoundException("Error en subir el video: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error en subir el video: {}", e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        }
    }
}