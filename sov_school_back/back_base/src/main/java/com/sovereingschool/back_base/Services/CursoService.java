package com.sovereingschool.back_base.Services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Interfaces.ICursoService;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
@Transactional
public class CursoService implements ICursoService {

    @Autowired
    private CursoRepository cursoRepo;

    @Autowired
    private ClaseRepository claseRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${variable.BACK_STREAM}")
    private String backStreamURL;

    @Value("${variable.BACK_CHAT}")
    private String backChatURL;

    private Path baseUploadDir;

    @PersistenceContext
    private EntityManager entityManager;

    public CursoService(
            @Value("${variable.VIDEOS_DIR}") String uploadDir,
            EntityManager entityManager) {
        this.baseUploadDir = Paths.get(uploadDir);
        this.entityManager = entityManager;
    }

    @Override
    public Long createCurso(Curso new_curso) {
        new_curso.setId_curso(null);
        Curso res = this.cursoRepo.save(new_curso);
        return res.getId_curso();
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
    public Curso getCurso(Long id_curso) {
        return this.cursoRepo.findById(id_curso).orElseThrow(() -> {
            System.err.println("Error en obtener el curso con ID " + id_curso);
            return new EntityNotFoundException("Error en obtener el curso con ID " + id_curso);
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
    public String getNombreCurso(Long id_curso) {
        return this.cursoRepo.findNombreCursoById(id_curso)
                .orElseThrow(() -> {
                    System.err.println("Error en obtener el nombre del curso con ID " + id_curso);
                    return new EntityNotFoundException("Error en obtener el nombre del curso con ID " + id_curso);
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
    public List<Usuario> getProfesoresCurso(Long id_curso) {
        List<Usuario> profesores = this.cursoRepo.findProfesoresCursoById(id_curso);
        if (profesores == null || profesores.isEmpty()) {
            System.err.println("Error en obtener los profesores del curso con ID " + id_curso);
            throw new EntityNotFoundException("Error en obtener los profesores del curso con ID " + id_curso);
        }
        return this.cursoRepo.findProfesoresCursoById(id_curso);
    }

    /**
     * Función para obtener la fecha de creación del curso
     * 
     * @param id_curso ID del curso
     * @return Date con la fecha de creación del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    @Override
    public Date getFechaCreacionCurso(Long id_curso) {
        return this.cursoRepo.findFechaCreacionCursoById(id_curso).orElseThrow(() -> {
            System.err.println("Error en obtener la fecha de creación del curso con ID " + id_curso);
            return new EntityNotFoundException("Error en obtener la fecha de creación del curso con ID " + id_curso);
        });
    }

    @Override
    public List<Clase> getClasesDelCurso(Long id_curso) {
        return this.cursoRepo.findClasesCursoById(id_curso);
    }

    @Override
    public List<Plan> getPlanesDelCurso(Long id_curso) {
        return this.cursoRepo.findPlanesCursoById(id_curso);
    }

    /**
     * Función para obtener el precio del curso
     * 
     * @param id_curso ID del curso
     * @return BigDecimal con el precio del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    @Override
    public BigDecimal getPrecioCurso(Long id_curso) {
        return this.cursoRepo.findPrecioCursoById(id_curso).orElseThrow(() -> {
            System.err.println("Error en obtener el precio del curso con ID " + id_curso);
            return new EntityNotFoundException("Error en obtener el precio del curso con ID " + id_curso);
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
        List<Clase> clases = curso.getClases_curso();
        curso.setClases_curso(null);
        // Si el curso no existe, crear un nuevo
        if (curso.getId_curso().equals(0L)) {
            curso.setId_curso(null);
            curso = this.cursoRepo.save(curso);
        }
        // Crear la carpeta del curso si no existe
        Path cursoPath = baseUploadDir.resolve(curso.getId_curso().toString());
        File cursoFile = new File(cursoPath.toString());
        if (!cursoFile.exists() || !cursoFile.isDirectory()) {
            if (!cursoFile.mkdir()) {
                System.err.println("Error en crear la carpeta del curso.");
                throw new RuntimeException("Error en crear la carpeta del curso.");
            }
        }

        // Crear las clases del curso si no existen
        if (clases.size() > 0) {
            for (Clase clase : clases) {
                clase.setCurso_clase(curso);
                if (clase.getId_clase().equals(0L)) {
                    clase.setId_clase(null);
                }

                try {
                    clase = this.claseRepo.save(clase);
                } catch (Exception e) {
                    System.err.println("Error en guardar la clase: " + e.getMessage());
                    throw new RuntimeException("Error en guardar la clase: " + e.getMessage());
                }

                // Crea la carpeta de la clase si no existe
                Path clasePath = baseUploadDir.resolve(curso.getId_curso().toString())
                        .resolve(clase.getId_clase().toString());
                File claseFile = new File(clasePath.toString());
                if (!claseFile.exists() || !claseFile.isDirectory()) {
                    if (!claseFile.mkdir()) {
                        throw new RuntimeException("Error en crear la carpeta de la clase");
                    }
                }
            }
            curso.setClases_curso(clases);
            try {
                curso = this.cursoRepo.save(curso);
            } catch (Exception e) {
                System.err.println("Error en actualizar el curso: " + e.getMessage());
                throw new RuntimeException("Error en actualizar el curso: " + e.getMessage());
            }
        }

        // Actualizar el chat del curso
        try {
            WebClient webClient = createSecureWebClient(backChatURL);
            webClient.post().uri("/actualizar_curso_chat")
                    .body(Mono.just(curso), Curso.class)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                System.err.println("Error HTTP del microservicio de chat: " + errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        System.err.println("Error al conectar con el microservicio de chat " + e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        if (res == null || !res.equals("Curso chat actualizado con éxito!!!")) {
                            System.err.println("Error en actualizar el curso en el chat");
                            System.err.println(res);
                            throw new RuntimeException("Error en actualizar el curso en el chat");
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error en actualizar el curso en el chat: " + e.getMessage());
            throw new RuntimeException("Error en actualizar el curso en el chat: " + e.getMessage());
        }

        // Actializa el curso en el stream
        try {
            WebClient webClient = createSecureWebClient(backStreamURL);
            webClient.post().uri("/actualizar_curso_stream")
                    .body(Mono.just(curso), Curso.class)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                System.err.println("Error HTTP del microservicio de streaming: " + errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        System.err.println("Error al conectar con el microservicio de streaming: " + e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        if (res == null || !res.equals("Curso stream actualizado con éxito!!!")) {
                            System.err.println("Error en actualizar el curso en el streaming:");
                            System.err.println(res);
                            throw new RuntimeException("Error en actualizar el curso en el streaming");
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error en actualizar el curso en el streaming: " + e.getMessage());
            throw new RuntimeException("Error en actualizar el curso en el streaming: " + e.getMessage());
        }
        // Si todo ha ido bien, devuelve el curso
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
    public Boolean deleteCurso(Long id_curso) {
        this.cursoRepo.findById(id_curso).orElseThrow(() -> {
            System.err.println("Error en obtener el curso con ID " + id_curso);
            return new EntityNotFoundException("Error en obtener el curso con ID " + id_curso);
        });
        if (this.getCurso(id_curso).getClases_curso() != null) {
            for (Clase clase : this.getCurso(id_curso).getClases_curso()) {
                this.deleteClase(clase);
            }
        }
        this.cursoRepo.deleteById(id_curso);

        Path cursoPath = Paths.get(this.baseUploadDir.toString(), id_curso.toString());
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
                System.err.println("Error al borrar la carpeta del curso: " + e.getMessage());
                throw new RuntimeException("Error al borrar la carpeta del curso: " + e.getMessage());
            }
        } else {
            System.err.println("La carpeta del curso no existe.");
        }

        // Eliminar el curso del microservicio de streaming
        try {
            WebClient webClient = createSecureWebClient(backStreamURL);
            webClient.delete()
                    .uri("/deleteCurso/" + id_curso)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                System.err.println("Error HTTP del microservicio de stream: " + errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(Boolean.class)
                    .onErrorResume(e -> {
                        System.err.println("Error al conectar con el microservicio de streaming: " + e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    })
                    .subscribe(res -> {
                        if (res == null || !res) {
                            System.err.println("Error en borrar el curso en el servicio de reproducción");
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error al conectar con el microservicio de streaming: " + e.getMessage());
            throw new RuntimeException("Error al conectar con el microservicio de streaming: " + e.getMessage());
        }

        // Eliminar el curso del microservicio de chat
        try {
            WebClient webClientChat = createSecureWebClient(backChatURL);
            webClientChat.delete()
                    .uri("/delete_curso_chat/" + id_curso)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                System.err.println("Error HTTP del microservicio de chat: " + errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        System.err.println("Error al conectar con el microservicio de chat: " + e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    })
                    .subscribe(res -> {
                        if (res == null || !res.equals("Curso chat borrado con exito!!!")) {
                            System.err.println("Error al eliminar el curso del microservicio de chat");
                            System.err.println(res);
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error al conectar con el microservicio de chat: " + e.getMessage());
            throw new RuntimeException("Error al conectar con el microservicio de chat: " + e.getMessage());
        }
        return true;
    }

    @Override
    public List<Curso> getAll() {
        return this.cursoRepo.findAll();
    }

    @Override
    public void deleteClase(Clase clase) {
        Optional<Clase> optionalClase = this.claseRepo.findById(clase.getId_clase());
        if (optionalClase.isPresent()) {
            this.claseRepo.delete(clase);
            if (clase.getDireccion_clase().length() > 0) {
                try {
                    Path path = Paths.get(clase.getDireccion_clase()).getParent();
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
                        System.err.println("La carpeta de la clase no existe.");
                    }
                } catch (Exception e) {
                    System.err.println("Error en borrar el video: " + e.getMessage());
                }
            }

            // Eliminar la carpeta de la clase
            try {
                // Obtener token
                WebClient webClient = createSecureWebClient(backStreamURL);
                webClient.delete()
                        .uri("/deleteClase/" + clase.getCurso_clase().getId_curso().toString() + "/"
                                + clase.getId_clase().toString())
                        .retrieve()
                        .onStatus(
                                status -> status.isError(),
                                response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                    System.err.println("Error HTTP del microservicio de stream: " + errorBody);
                                    return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                                }))
                        .bodyToMono(Boolean.class)
                        .onErrorResume(e -> {
                            // Manejo de errores
                            System.err
                                    .println("Error al conectar con el microservicio de streaming: " + e.getMessage());
                            return Mono.empty(); // Continuar sin interrumpir la aplicación

                        }).subscribe(res -> {
                            // Maneja el resultado cuando esté disponible
                            if (res == null || !res) {
                                System.err.println("Error en actualizar el curso en el servicio de reproducción");
                                System.err.println(res);
                            }
                        });
            } catch (Exception e) {
                System.err.println("error en borrar la clase en el streaming: " + e.getMessage());
            }

            // Elimina el chat de la clase
            try {
                // Obtener token
                WebClient webClient = createSecureWebClient(backChatURL);
                webClient.delete()
                        .uri("/delete_clase_chat/" + clase.getCurso_clase().getId_curso().toString() + "/"
                                + clase.getId_clase().toString())
                        .retrieve()
                        .onStatus(
                                status -> status.isError(),
                                response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                    System.err.println("Error HTTP del microservicio de chat: " + errorBody);
                                    return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                                }))
                        .bodyToMono(String.class)
                        .onErrorResume(e -> {
                            System.err.println("Error al conectar con el microservicio de chat: " + e.getMessage());
                            return Mono.empty(); // Continuar sin interrumpir la aplicación
                        }).subscribe(res -> {
                            if (res == null || !res.equals("Clase chat borrado con exito!!!")) {
                                System.err.println("Error en borrar la clase en el chat");
                                System.err.println(res);
                            }
                        });
            } catch (Exception e) {
                System.err.println("Error en borrar la clase en el chat: " + e.getMessage());
            }
            // TODO: Mirar si se necesita eliminar algo en el microservicio de streaming
        } else {
            System.err.println("Clase no encontrada con ID: " + clase.getId_clase());
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
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new AccessDeniedException("Error en subir el video: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new IllegalArgumentException("Error en subir el video: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new EntityNotFoundException("Error en subir el video: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error en subir el video: " + e.getMessage());
            throw new RuntimeException("Error en subir el video: " + e.getMessage());
        }
    }

    private WebClient createSecureWebClient(String baseUrl) throws Exception {
        URI uri = new URI(baseUrl);
        String host = uri.getHost(); // p.ej. "sovschool-back-chat"
        int port = uri.getPort(); // p.ej. 8070

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        String authToken = this.jwtUtil.generateToken(null, "server", null);

        String hostHeader = (port == 443 || port == -1)
                ? host
                : host + ":" + port;

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.HOST, hostHeader)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .build();
    }
}