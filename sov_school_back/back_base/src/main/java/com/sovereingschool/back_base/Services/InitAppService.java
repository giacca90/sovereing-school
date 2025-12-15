package com.sovereingschool.back_base.Services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.DTOs.CursosInit;
import com.sovereingschool.back_base.DTOs.Estadistica;
import com.sovereingschool.back_base.DTOs.InitApp;
import com.sovereingschool.back_base.DTOs.ProfesInit;
import com.sovereingschool.back_base.Interfaces.IInitAppService;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class InitAppService implements IInitAppService {

    private CursoRepository cursoRepo;
    private ClaseRepository claseRepo;
    private UsuarioRepository usuarioRepo;
    private JwtUtil jwtUtil;
    private WebClientConfig webClientConfig;

    private Logger logger = LoggerFactory.getLogger(InitAppService.class);

    private String frontDocker;

    /**
     * Constructor de InitAppService
     *
     * @param frontDocker     URL del microservicio de front
     * @param cursoRepo       Repositorio de cursos
     * @param claseRepo       Repositorio de clases
     * @param usuarioRepo     Repositorio de usuarios
     * @param jwtUtil         Utilidad de JWT
     * @param webClientConfig Configuración de WebClient
     */
    public InitAppService(@Value("${variable.FRONT_DOCKER}") String frontDocker,
            CursoRepository cursoRepo,
            ClaseRepository claseRepo,
            UsuarioRepository usuarioRepo,
            JwtUtil jwtUtil,
            WebClientConfig webClientConfig) {
        this.frontDocker = frontDocker;
        this.cursoRepo = cursoRepo;
        this.claseRepo = claseRepo;
        this.usuarioRepo = usuarioRepo;
        this.jwtUtil = jwtUtil;
        this.webClientConfig = webClientConfig;
    }

    /**
     * Función para obtener los profesores
     * 
     * @return Lista de Usuarios
     */
    @Override
    public List<Usuario> getProfesores() {
        return this.usuarioRepo.getInit();
    }

    /**
     * Función para obtener la información inicial
     * 
     * @return InitApp
     */
    @Override
    public InitApp getInit() {
        List<Usuario> profes = this.usuarioRepo.getInit();
        List<ProfesInit> profesInit = new ArrayList<>();
        profes.forEach(profe -> {
            ProfesInit init = new ProfesInit(
                    profe.getIdUsuario(),
                    profe.getNombreUsuario(),
                    profe.getFotoUsuario(),
                    profe.getPresentacion());
            profesInit.add(init);
        });

        List<Curso> cursos = this.cursoRepo.getAllCursos();
        List<CursosInit> cursosInit = new ArrayList<>();
        cursos.forEach(curso -> {
            CursosInit init = new CursosInit(
                    curso.getIdCurso(),
                    curso.getNombreCurso(),
                    curso.getProfesoresCurso().stream().map(Usuario::getIdUsuario).toList(),
                    curso.getDescripcionCorta(),
                    curso.getImagenCurso());
            cursosInit.add(init);
        });

        Estadistica estadistica = new Estadistica(
                profesInit.size(),
                this.usuarioRepo.count() - profesInit.size(),
                this.cursoRepo.count(),
                this.claseRepo.count());

        return new InitApp(
                cursosInit,
                profesInit,
                estadistica);
    }

    /**
     * Función para obtener el token de inicialización
     * 
     * @return String con el token de inicialización
     */
    @Override
    public String getInitToken() {
        return this.jwtUtil.generateInitToken();
    }

    /**
     * Función para actualizar el SSR
     * 
     * @throws InternalComunicationException
     */
    @Override
    public void refreshSSR() throws InternalComunicationException {
        try {
            InitApp init = this.getInit();

            WebClient webClient = webClientConfig.createSecureWebClient(frontDocker);

            webClient.post()
                    .uri("/refresh-cache")
                    .body(Mono.just(init), InitApp.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del SSR: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del SSR: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio del front: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    })
                    .subscribe(res -> {
                        if (res == null || !res.contains("Cache global actualizado con éxito")) {
                            logger.error("Error en actualizar el cache global");
                            logger.error(res);
                            throw new RuntimeException("Error en actualizar el cache global");
                        }
                    });

        } catch (Exception e) {
            throw new InternalComunicationException("Error al actualizar el SSR: " + e.getMessage());
        }
    }

}
