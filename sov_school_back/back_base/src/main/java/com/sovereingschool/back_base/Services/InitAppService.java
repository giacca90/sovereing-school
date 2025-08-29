package com.sovereingschool.back_base.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CursoRepository cursoRepo;

    @Autowired
    private ClaseRepository claseRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebClientConfig webClientConfig;

    @Value("${variable.FRONT}")
    private String frontURL;

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
        String initToken = this.jwtUtil.generateInitToken();
        return initToken;
    }

    @Override
    public void refreshSSR() {
        try {
            InitApp init = this.getInit();

            // URL del contenedor del SSR
            WebClient webClient = webClientConfig.createSecureWebClient("https://sovschool-front:4200");

            webClient.post()
                    .uri("/refresh-cache")
                    .body(Mono.just(init), InitApp.class)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                System.err.println("Error HTTP del SSR: " + errorBody);
                                return Mono.error(new RuntimeException("Error del SSR: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        System.err.println("Error al conectar con el microservicio del front: " + e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    })
                    .subscribe(res -> {
                        if (res == null || !res.contains("Cache global actualizado con éxito!!!")) {
                            System.err.println("Error en actualizar el cache global");
                            System.err.println(res);
                            throw new RuntimeException("Error en actualizar el cache global");
                        }
                    });

        } catch (Exception e) {
            System.err.println("Error al actualizar el SSR: " + e.getMessage());
        }
    }

}
