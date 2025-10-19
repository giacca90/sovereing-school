package com.sovereingschool.back_base.Services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.DTOs.CursosInit;
import com.sovereingschool.back_base.DTOs.Estadistica;
import com.sovereingschool.back_base.DTOs.InitApp;
import com.sovereingschool.back_base.DTOs.ProfesInit;
import com.sovereingschool.back_base.Interfaces.IInitAppService;
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

    @Value("${variable.FRONT_DOCKER}")
    private String frontDocker;

    public InitAppService(CursoRepository cursoRepo, ClaseRepository claseRepo, UsuarioRepository usuarioRepo,
            JwtUtil jwtUtil, WebClientConfig webClientConfig) {
        this.cursoRepo = cursoRepo;
        this.claseRepo = claseRepo;
        this.usuarioRepo = usuarioRepo;
        this.jwtUtil = jwtUtil;
        this.webClientConfig = webClientConfig;
    }

    @Override
    public List<Usuario> getProfesores() {
        return this.usuarioRepo.getInit();
    }

    @Override
    public InitApp getInit() {
        List<Usuario> profes = this.usuarioRepo.getInit();
        List<ProfesInit> profesInit = new ArrayList<>();
        profes.forEach(profe -> {
            ProfesInit init = new ProfesInit();
            init.setId_usuario(profe.getId_usuario());
            init.setNombre_usuario(profe.getNombre_usuario());
            init.setFoto_usuario(profe.getFoto_usuario());
            init.setPresentacion(profe.getPresentacion());
            profesInit.add(init);

        });

        List<Curso> cursos = this.cursoRepo.getAllCursos();
        List<CursosInit> cursosInit = new ArrayList<>();
        cursos.forEach(curso -> {
            CursosInit init = new CursosInit();
            init.setId_curso(curso.getId_curso());
            init.setNombre_curso(curso.getNombre_curso());
            init.setImagen_curso(curso.getImagen_curso());
            init.setDescriccion_corta(curso.getDescriccion_corta());
            List<Long> ids_profes = new ArrayList<>();
            curso.getProfesores_curso().forEach(profe -> {
                ids_profes.add(profe.getId_usuario());
            });

            init.setProfesores_curso(ids_profes);
            cursosInit.add(init);
        });

        Estadistica estadistica = new Estadistica();
        estadistica.setClases(this.claseRepo.count());
        estadistica.setCursos(this.cursoRepo.count());
        estadistica.setProfesores(profesInit.size());
        estadistica.setAlumnos(this.usuarioRepo.count() - profesInit.size());

        InitApp init = new InitApp();
        init.setCursosInit(cursosInit);
        init.setProfesInit(profesInit);
        init.setEstadistica(estadistica);

        return init;

    }

    @Override
    public String getInitToken() {
        return this.jwtUtil.generateInitToken();
    }

    @Override
    public void refreshSSR() {
        try {
            InitApp init = this.getInit();

            WebClient webClient = webClientConfig.createSecureWebClient(frontDocker);

            webClient.post()
                    .uri("/refresh-cache")
                    .body(Mono.just(init), InitApp.class)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
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
            logger.error("Error al actualizar el SSR: {}", e.getMessage());
        }
    }

}
