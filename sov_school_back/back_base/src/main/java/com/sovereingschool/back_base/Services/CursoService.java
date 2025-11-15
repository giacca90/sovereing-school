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
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Exceptions.ServiceException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;

/**
 * Servicio de gestión de cursos, implementando la interfaz ICursoService
 * 
 * 
 * 
 */
@Service
@Transactional
public class CursoService implements ICursoService {

    private CursoRepository cursoRepo;
    private ClaseRepository claseRepo;
    private InitAppService initAppService;
    private WebClientConfig webClientConfig;

    private Logger logger = LoggerFactory.getLogger(CursoService.class);

    private String backStreamURL;

    private String backChatURL;

    private Path baseUploadDir;

    /**
     * Constructor de CursoService
     *
     * @param uploadDir       Ruta de carga de archivos
     * @param backStreamURL   URL del microservicio de streaming
     * @param backChatURL     URL del microservicio de chat
     * @param cursoRepo       Repositorio de cursos
     * @param claseRepo       Repositorio de clases
     * @param initAppService  Servicio de inicialización
     * @param webClientConfig Configuración de WebClient
     */
    public CursoService(
            @Value("${variable.VIDEOS_DIR}") String uploadDir,
            @Value("${variable.BACK_STREAM_DOCKER}") String backStreamURL,
            @Value("${variable.BACK_CHAT_DOCKER}") String backChatURL,
            CursoRepository cursoRepo,
            ClaseRepository claseRepo,
            InitAppService initAppService,
            WebClientConfig webClientConfig) {
        this.backStreamURL = backStreamURL;
        this.backChatURL = backChatURL;
        this.baseUploadDir = Paths.get(uploadDir);
        this.cursoRepo = cursoRepo;
        this.claseRepo = claseRepo;
        this.initAppService = initAppService;
        this.webClientConfig = webClientConfig;
    }

    @Override
    public Long createCurso(Curso newCurso) throws RepositoryException {
        try {
            newCurso.setIdCurso(null);
            Curso res = this.cursoRepo.save(newCurso);
            return res.getIdCurso();
        } catch (Exception e) {
            throw new RepositoryException("Error en crear el curso: " + e.getMessage(), e);
        }
    }

    /**
     * Función para obtener un curso
     * 
     * @param id_curso ID del curso
     * @return Curso con los datos del curso
     * @throws NotFoundException si el curso no existe
     * 
     */
    @Override
    public Curso getCurso(Long idCurso) throws NotFoundException {
        return this.cursoRepo.findById(idCurso).orElseThrow(() -> {
            logger.error("CursoService: getCurso: Error en obtener el curso con ID {}: ", idCurso);
            return new NotFoundException("Error en obtener el curso con ID " + idCurso);
        });
    }

    /**
     * Función para obtener el nombre del curso
     * 
     * @param id_curso ID del curso
     * @return String con el nombre del curso
     * @throws NotFoundException si el curso no existe
     * 
     */
    @Override
    public String getNombreCurso(Long idCurso) throws NotFoundException {
        return this.cursoRepo.findNombreCursoById(idCurso)
                .orElseThrow(() -> {
                    logger.error("Error en obtener el nombre del curso con ID {}", idCurso);
                    return new NotFoundException("Error en obtener el nombre del curso con ID " + idCurso);
                });
    }

    /**
     * Función para obtener los profesores del curso
     * 
     * @param id_curso ID del curso
     * @return Lista de usuarios con los profesores del curso
     * @throws NotFoundException si el curso no existe
     * 
     */
    @Override
    public List<Usuario> getProfesoresCurso(Long idCurso) throws NotFoundException {
        List<Usuario> profesores = this.cursoRepo.findProfesoresCursoById(idCurso);
        if (profesores == null || profesores.isEmpty()) {
            throw new NotFoundException("Error en obtener los profesores del curso con ID " + idCurso);
        }
        return this.cursoRepo.findProfesoresCursoById(idCurso);
    }

    /**
     * Función para obtener la fecha de creación del curso
     * 
     * @param id_curso ID del curso
     * @return Date con la fecha de creación del curso
     * @throws NotFoundException si el curso no existe
     * 
     */
    @Override
    public Date getFechaCreacionCurso(Long idCurso) throws NotFoundException {
        return this.cursoRepo.findFechaCreacionCursoById(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener la fecha de creación del curso con ID {}", idCurso);
            return new NotFoundException("Error en obtener la fecha de creación del curso con ID " + idCurso);
        });
    }

    /**
     * Función para obtener las clases del curso
     * 
     * @param id_curso ID del curso
     * @return Lista de Clase con las clases del curso
     * @throws NotFoundException si no hay clases del curso
     * 
     */
    @Override
    public List<Clase> getClasesDelCurso(Long idCurso) throws NotFoundException {
        List<Clase> clases = this.cursoRepo.findClasesCursoById(idCurso);
        if (clases == null || clases.isEmpty()) {
            logger.error("Error en obtener las clases del curso con ID {}", idCurso);
            throw new NotFoundException("Error en obtener las clases del curso con ID " + idCurso);
        }
        return clases;
    }

    /**
     * Función para obtener los planes del curso
     * 
     * @param id_curso ID del curso
     * @return Lista de Plan con los planes del curso
     * @throws NotFoundException si no hay planes del curso
     * 
     */
    @Override
    public List<Plan> getPlanesDelCurso(Long idCurso) throws NotFoundException {
        List<Plan> planes = this.cursoRepo.findPlanesCursoById(idCurso);
        if (planes == null || planes.isEmpty()) {
            logger.error("Error en obtener los planes del curso con ID {}", idCurso);
            throw new NotFoundException("Error en obtener los planes del curso con ID " + idCurso);
        }
        return planes;
    }

    /**
     * Función para obtener el precio del curso
     * 
     * @param id_curso ID del curso
     * @return BigDecimal con el precio del curso
     * @throws NotFoundException si el curso no existe
     *
     */
    @Override
    public BigDecimal getPrecioCurso(Long idCurso) throws NotFoundException {
        return this.cursoRepo.findPrecioCursoById(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener el precio del curso con ID {}", idCurso);
            return new NotFoundException("Error en obtener el precio del curso con ID " + idCurso);
        });
    }

    /**
     * 
     * Función para actualizar o crear un nuevo curso
     * 
     * @param curso Curso: curso a actualizar
     * @return Curso con los datos actualizados
     * @throws ServiceException si ocurre un error en el servidor
     * 
     */
    @Override
    public Curso updateCurso(Curso curso)
            throws NotFoundException, InternalServerException, InternalComunicationException, RepositoryException {

        List<Clase> clases = curso.getClasesCurso();
        curso.setClasesCurso(null);
        // Si el curso no existe, crear un nuevo
        if (curso.getIdCurso().equals(0L)) {
            curso.setIdCurso(null);
            curso = this.cursoRepo.save(curso);
        }
        // Crear la carpeta del curso si no existe
        this.creaCarpetaCurso(curso);

        // Crear las clases del curso si no existen
        this.creaClasesCurso(curso, clases);
        // Actualizar el chat del curso
        this.actualizarChatCurso(curso);

        // Actializa el curso en el stream
        this.actualizarStreamCurso(curso);

        // Actualizar el SSR
        this.updateSSR();

        return curso;
    }

    /**
     * Función para eliminar un curso
     * 
     * @param id_curso ID del curso
     * @return Boolean con el resultado de la operación
     * @throws ServiceException
     * @throws EntityNotFoundException si el curso no existe
     * @throws RuntimeException        si ocurre un error en el servidor
     * 
     */
    @Override
    public Boolean deleteCurso(Long idCurso) throws ServiceException {
        try {
            if (this.getCurso(idCurso).getClasesCurso() != null) {
                for (Clase clase : this.getCurso(idCurso).getClasesCurso()) {
                    this.deleteClase(clase);
                }
            }
        } catch (Exception e) {
            logger.error("CursoService: deleteCurso: Error en obtener el curso con ID {}: ", idCurso);
            throw new EntityNotFoundException("Error en obtener el curso con ID " + idCurso);
        }
        this.cursoRepo.deleteById(idCurso);

        // Eliminar la carpeta del curso
        this.deleteCarpetaCurso(idCurso);

        // Eliminar el curso del microservicio de streaming
        this.deleteCursoStream(idCurso);

        // Eliminar el curso del microservicio de chat
        this.deleteCursoChat(idCurso);

        // Actualizar el SSR
        this.updateSSR();
        return true;
    }

    @Override
    public List<Curso> getAll() {
        return this.cursoRepo.findAll();
    }

    @Override
    public void deleteClase(Clase clase) throws ServiceException {
        Optional<Clase> optionalClase = this.claseRepo.findById(clase.getIdClase());
        if (!optionalClase.isPresent()) {
            throw new IllegalArgumentException("Clase no encontrada con id " + clase.getIdClase());
        }

        this.claseRepo.delete(clase);
        // Eliminar la carpeta de la clase
        this.deleteCarpetaClase(clase);

        // Elimina la clase del microservicio de streaming
        this.deleteClaseStream(clase);

        // Elimina el chat de la clase
        this.deleteClaseChat(clase);

        // Actualizar el SSR
        this.updateSSR();
    }

    /**
     * Función para subir un video
     * 
     * @param file Archivo subido
     * @return String con la ruta del archivo subido
     * @throws InternalServerException
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     * @throws EntityNotFoundException  si el curso no existe
     * @throws IllegalArgumentException si el curso no tiene un ID
     * @throws IllegalStateException    si el curso no tiene un ID
     * @throws IOException              si ocurre un error en el servidor
     * @throws RuntimeException         si ocurre un error en el servidor
     */
    @Override
    public String subeVideo(MultipartFile file) throws InternalServerException {
        try {
            // Nombre original o valor por defecto
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) {
                originalFileName = "unknown_file";
            }

            // Limpieza del nombre y path final
            String cleanedFileName = StringUtils.cleanPath(originalFileName);
            Path filePath = baseUploadDir.resolve(UUID.randomUUID() + "_" + cleanedFileName);

            // Guarda el archivo
            Files.write(filePath, file.getBytes());

            return filePath.normalize().toString();

        } catch (AccessDeniedException e) {
            throw new InternalServerException("Permiso denegado al subir el video: " + file.getOriginalFilename(), e);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InternalServerException("Error al subir el video: " + file.getOriginalFilename(), e);

        } catch (IOException e) {
            throw new InternalServerException("Error de IO al subir el video: " + file.getOriginalFilename(), e);

        } catch (RuntimeException e) {
            throw new InternalServerException("Error inesperado al subir el video: " + e.getMessage(), e);
        }
    }

    /**
     * Función para crear la carpeta del curso
     * 
     * @param curso Curso a crear la carpeta
     * @throws InternalServerException
     */
    protected void creaCarpetaCurso(Curso curso) throws InternalServerException {
        Path cursoPath = baseUploadDir.resolve(curso.getIdCurso().toString());
        File cursoFile = new File(cursoPath.toString());
        if (!cursoFile.exists() || !cursoFile.isDirectory() && !cursoFile.mkdir()) {
            logger.error("Error en crear la carpeta del curso.");
            throw new InternalServerException("Error en crear la carpeta del curso.");
        }
    }

    /**
     * Función para crear las clases del curso
     * 
     * @param curso  Curso a crear las clases
     * @param clases Lista de Clases a crear
     * @throws RepositoryException
     * @throws InternalServerException
     */
    protected void creaClasesCurso(Curso curso, List<Clase> clases)
            throws RepositoryException, InternalServerException {
        if (clases.isEmpty()) {
            for (Clase clase : clases) {
                clase.setCursoClase(curso);
                if (clase.getIdClase().equals(0L)) {
                    clase.setIdClase(null);
                }
                try {
                    clase = this.claseRepo.save(clase);
                } catch (IllegalArgumentException e) {
                    throw new RepositoryException("Error en guardar la clase: " + e.getMessage(), e);
                }

                // Crea la carpeta de la clase si no existe
                this.creaCarpetaClase(curso, clase);
            }
            curso.setClasesCurso(clases);
            try {
                this.cursoRepo.save(curso);
            } catch (IllegalArgumentException e) {
                throw new RepositoryException("Error en actualizar el curso: " + e.getMessage());
            }
        }
    }

    /**
     * Función para actualizar el chat del curso
     * 
     * @param curso Curso a actualizar el chat
     * @throws InternalComunicationException
     * @throws InternalServerException
     */
    protected void actualizarChatCurso(Curso curso) throws InternalComunicationException {
        try {
            WebClient webClient = webClientConfig.createSecureWebClient(backChatURL);
            webClient.post().uri("/actualizar_curso_chat")
                    .body(Mono.just(curso), Curso.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new InternalComunicationException(
                                            "Error del microservicio de chat: " + errorBody))))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de chat {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        if (res == null || !res.equals("Curso chat actualizado con éxito!!!")) {
                            logger.error("Error en actualizar el curso en el chat");
                            logger.error(res);
                            // Log error and continue instead of throwing
                            logger.error("Error en actualizar el curso en el chat");
                        }
                    });
        } catch (Exception e) {
            throw new InternalComunicationException("Error en actualizar el curso en el chat: " + e.getMessage(), e);
        }
    }

    /**
     * Función para actualizar el streaming del curso
     * 
     * @param curso Curso a actualizar el streaming
     * @throws InternalComunicationException
     * @throws InternalServerException
     */
    protected void actualizarStreamCurso(Curso curso) throws InternalComunicationException {
        try {
            WebClient webClient = webClientConfig.createSecureWebClient(backStreamURL);
            webClient.post().uri("/actualizar_curso_stream")
                    .body(Mono.just(curso), Curso.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de streaming: {}", errorBody);
                                return Mono.error(new InternalComunicationException(
                                        "Error del microservicio de streaming: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de streaming: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        if (res == null || !res.equals("Curso stream actualizado con éxito!!!")) {
                            logger.error("Error en actualizar el curso en el streaming:");
                            logger.error(res);
                        }
                    });
        } catch (Exception e) {
            throw new InternalComunicationException("Error en actualizar el curso en el streaming: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Función para actualizar el SSR
     * 
     * @throws InternalComunicationException
     */
    protected void updateSSR() throws InternalComunicationException {
        try {
            this.initAppService.refreshSSR();
        } catch (Exception e) {
            throw new InternalComunicationException("Error en actualizar el SSR: " + e.getMessage(), e);
        }
    }

    /**
     * Función para eliminar el streaming del curso
     * 
     * @param idCurso id del curso
     * @throws InternalComunicationException
     */
    protected void deleteCursoStream(Long idCurso) throws InternalComunicationException {
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
            throw new InternalComunicationException(
                    "Error al conectar con el microservicio de streaming: " + e.getMessage(), e);
        }
    }

    /**
     * Función para eliminar el chat del curso
     * 
     * @param idCurso id del curso
     * @throws InternalComunicationException
     */
    protected void deleteCursoChat(Long idCurso) throws InternalComunicationException {
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
            throw new InternalComunicationException("Error al conectar con el microservicio de chat: " + e.getMessage(),
                    e);
        }

    }

    /**
     * Función para crear la carpeta de la clase
     * 
     * @param curso Curso a crear la carpeta
     * @param clase Clase a crear la carpeta
     * @throws InternalServerException
     */
    protected void creaCarpetaClase(Curso curso, Clase clase) throws InternalServerException {
        Path clasePath = baseUploadDir.resolve(curso.getIdCurso().toString())
                .resolve(clase.getIdClase().toString());
        File claseFile = new File(clasePath.toString());
        if (!claseFile.exists() || !claseFile.isDirectory() && !claseFile.mkdir()) {
            throw new InternalServerException("Error en crear la carpeta de la clase");
        }
    }

    /**
     * Función para eliminar la carpeta de la clase
     * 
     * @param clase Clase a eliminar la carpeta
     * @throws InternalServerException
     */
    protected void deleteCarpetaClase(Clase clase) throws InternalServerException {
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
                    throw new NotFoundException("La carpeta del la clase " + clase.getIdClase() + " no existe");
                }
            } catch (Exception e) {
                throw new InternalServerException("Error en borrar el video: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Función para eliminar la carpeta del curso
     * 
     * @param idCurso id del curso
     * @throws InternalServerException
     */
    protected void deleteCarpetaCurso(Long idCurso) throws InternalServerException {
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
            } catch (IOException e) {
                throw new InternalServerException("Error al borrar la carpeta del curso: " + e.getMessage(), e);
            }
        } else {
            logger.error("La carpeta del curso no existe.");
        }
    }

    /**
     * Función para eliminar el streaming de la clase
     * 
     * @param clase Clase a eliminar el streaming
     * @throws InternalComunicationException
     * 
     *                                       TODO: Cambiar por Redis Stream
     */
    protected void deleteClaseStream(Clase clase) throws InternalComunicationException {
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
            throw new InternalComunicationException("Error en borrar la clase en el streaming: " + e.getMessage(), e);
        }
    }

    /**
     * Función para eliminar el chat de la clase
     * 
     * @param clase Clase a eliminar el chat
     * @throws InternalComunicationException
     * 
     *                                       TODO: Cambiar por Redis Stream
     */
    protected void deleteClaseChat(Clase clase) throws InternalComunicationException {
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
            throw new InternalComunicationException("Error en borrar la clase en el chat: " + e.getMessage(), e);
        }
    }
}