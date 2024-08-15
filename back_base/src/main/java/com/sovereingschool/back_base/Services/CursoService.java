package com.sovereingschool.back_base.Services;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Interfaces.ICursoService;
import com.sovereingschool.back_base.Models.Clase;
import com.sovereingschool.back_base.Models.Curso;
import com.sovereingschool.back_base.Models.Plan;
import com.sovereingschool.back_base.Models.Usuario;
import com.sovereingschool.back_base.Repositories.ClaseRepository;
import com.sovereingschool.back_base.Repositories.CursoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class CursoService implements ICursoService {

    @Autowired
    private CursoRepository repo;

    @Autowired
    private ClaseRepository claseRepo;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Long createCurso(Curso new_curso) {
        new_curso.setId_curso(null);
        Curso res = this.repo.save(new_curso);
        return res.getId_curso();
    }

    @Override
    public Curso getCurso(Long id_curso) {
        Optional<Curso> curso = this.repo.findById(id_curso);
        if (curso.isPresent()) {
            return curso.get();
        }
        return null;
    }

    @Override
    public String getNombreCurso(Long id_curso) {
        return this.repo.findNombreCursoById(id_curso);
    }

    @Override
    public List<Usuario> getProfesoresCurso(Long id_curso) {
        return this.repo.findProfesoresCursoById(id_curso);
    }

    @Override
    public Date getFechaCreacionCurso(Long id_curso) {
        return this.repo.findFechaCreacionCursoById(id_curso);
    }

    @Override
    public List<Clase> getClasesDelCurso(Long id_curso) {
        return this.repo.findClasesCursoById(id_curso);
    }

    @Override
    public List<Plan> getPlanesDelCurso(Long id_curso) {
        return this.repo.findPlanesCursoById(id_curso);
    }

    @Override
    public BigDecimal getPrecioCurso(Long id_curso) {
        return this.repo.findPrecioCursoById(id_curso);
    }

    @Override
    public Curso updateCurso(Curso curso) {
        Optional<Curso> respuesta = this.repo.findById(curso.getId_curso());
        if (!respuesta.isPresent())
            return null;
        Curso oldCurso = respuesta.get();
        for (Clase clase : oldCurso.getClases_curso()) {
            System.out.println("OLD: " + clase.getDireccion_clase());
        }
        for (Clase clase : curso.getClases_curso()) {
            System.out.println(clase.getDireccion_clase());
            if (!oldCurso.getClases_curso().contains(clase)) {
                if (clase.getId_clase() == 0) {
                    clase = this.claseRepo.save(clase);
                    WebClient webClient = webClientBuilder.baseUrl("http://localhost:8090").build();
                    webClient.put()
                            .uri("/addClase/" + curso.getId_curso())
                            .body(Mono.just(clase), Clase.class)
                            .retrieve()
                            .bodyToMono(Boolean.class)
                            .doOnError(e -> {
                                // Manejo de errores
                                System.err.println("ERROR: " + e.getMessage());
                                e.printStackTrace();
                            }).subscribe(res -> {
                                // Maneja el resultado cuando esté disponible
                                if (res != null && res) {
                                    System.out.println("Actualización exitosa");
                                } else {
                                    System.err.println("Error en actualizar el curso en el servicio de reproducción");
                                }
                            });
                } else {
                    this.claseRepo.updateClase(clase.getId_clase(), clase.getNombre_clase(), clase.getTipo_clase(),
                            clase.getDireccion_clase(), clase.getPosicion_clase());
                }
            }
        }
        return this.repo.save(curso);
    }

    @Override
    public String deleteCurso(Long id_curso) {
        if (!this.repo.findById(id_curso).isPresent()) {
            return null;
        }
        this.getCurso(id_curso).getClases_curso().forEach((clase) -> {
            this.deleteClase(clase);
        });
        this.repo.deleteById(id_curso);
        return "Curso eliminado con éxito!!!";
    }

    @Override
    public List<Curso> getAll() {
        return this.repo.findAll();
    }

    @Override
    public void deleteClase(Clase clase) {
        Optional<Clase> optionalClase = this.claseRepo.findById(clase.getId_clase());
        if (optionalClase.isPresent()) {
            this.claseRepo.delete(optionalClase.get());
            Path path = Paths.get(clase.getDireccion_clase());
            try {

                if (Files.exists(path)) {
                    Files.delete(path);
                }
            } catch (Exception e) {
                System.err.println("Error en borrar el video: " + e.getMessage());
            }
        } else {
            System.err.println("Clase no encontrada con ID: " + clase.getId_clase());
        }
    }

    @Override
    public void convertVideo(String inputFilePath, String outputDir) {
        // Construye el comando FFmpeg
        // Extrae el directorio y el nombre del archivo de entrada
        Path inputPath = Paths.get(inputFilePath);
        String inputFileName = inputPath.getFileName().toString();
        String inputDir = inputPath.getParent().toString();

        // Construye el comando FFmpeg
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFileName,
                "-master_pl_name", "master.m3u8",
                "-f", "hls",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", "360p_%03d.ts",
                "-vf", "scale=w=640:h=360:force_original_aspect_ratio=decrease",
                "-c:v", "libx264", "-b:v", "800k", "-preset", "fast", "-profile:v", "baseline",
                "-g", "48", "-keyint_min", "48", "-sc_threshold", "0", "-b:a", "96k", "-ac", "1", "-ar", "44100",
                "360p.m3u8",
                "-f", "hls",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", "480p_%03d.ts",
                "-vf", "scale=w=842:h=480:force_original_aspect_ratio=decrease",
                "-c:v", "libx264", "-b:v", "1200k", "-preset", "fast", "-profile:v", "baseline",
                "-g", "48", "-keyint_min", "48", "-sc_threshold", "0", "-b:a", "128k", "-ac", "1", "-ar", "44100",
                "480p.m3u8",
                "-f", "hls",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", "720p_%03d.ts",
                "-vf", "scale=w=1280:h=720:force_original_aspect_ratio=decrease",
                "-c:v", "libx264", "-b:v", "2500k", "-preset", "fast", "-profile:v", "baseline",
                "-g", "48", "-keyint_min", "48", "-sc_threshold", "0", "-b:a", "192k", "-ac", "1", "-ar", "44100",
                "720p.m3u8");

        // Establece el directorio de trabajo al directorio que contiene el archivo de
        // entrada
        processBuilder.directory(new java.io.File(inputDir));
        processBuilder.redirectErrorStream(true);

        // Inicia el proceso
        try {
            Process process = processBuilder.start();

            // Lee la salida del proceso para depuración
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Espera a que el proceso termine
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg process failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Error en convertir el video: " + e.getCause());
        }
    }
}