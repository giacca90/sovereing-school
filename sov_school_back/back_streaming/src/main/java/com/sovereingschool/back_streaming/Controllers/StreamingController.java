package com.sovereingschool.back_streaming.Controllers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_streaming.Services.StreamingService;
import com.sovereingschool.back_streaming.Services.UsuarioCursosService;

@RestController
@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
public class StreamingController {

    private static class LimitedInputStream extends java.io.InputStream {
        private final RandomAccessFile file;
        private long remaining;

        public LimitedInputStream(RandomAccessFile file, long remaining) {
            this.file = file;
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining > 0) {
                remaining--;
                return file.read();
            } else {
                return -1;
            }
        }

        @Override
        public void close() throws IOException {
            file.close();
            super.close();
        }

    }

    private UsuarioCursosService usuarioCursosService;
    private StreamingService streamingService;

    private Logger logger = LoggerFactory.getLogger(StreamingController.class);

    /**
     * Constructor de StreamingController
     *
     * @param usuarioCursosService Servicio de usuarios de cursos
     * @param streamingService     Servicio de streaming
     */
    public StreamingController(UsuarioCursosService usuarioCursosService,
            StreamingService streamingService) {
        this.usuarioCursosService = usuarioCursosService;
        this.streamingService = streamingService;
    }

    /**
     * Función para obtener las listas de un curso
     * 
     * @param idCurso ID del curso
     * @param idClase ID de la clase
     * @param lista   String con la lista a obtener
     * @param headers HttpHeaders con las cabeceras del request
     * @return ResponseEntity<String> con el resultado de la operación
     * @throws IOException
     * @throws InternalServerException
     */
    @GetMapping("/{idCurso}/{idClase}/{lista}")
    public ResponseEntity<?> getListas(@PathVariable Long idCurso,
            @PathVariable Long idClase,
            @PathVariable String lista,
            @RequestHeader HttpHeaders headers) throws IOException, InternalServerException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Error en el token de acceso", HttpStatus.UNAUTHORIZED);
        }
        Long idUsuario = (Long) authentication.getDetails();

        String direccionCarpeta = this.usuarioCursosService.getClase(idUsuario, idCurso, idClase);
        if (direccionCarpeta == null) {
            logger.error("No se encuentra la carpeta del curso: {}", direccionCarpeta);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No se encuentra la carpeta del curso: " + direccionCarpeta);
        }
        direccionCarpeta = direccionCarpeta.substring(0, direccionCarpeta.lastIndexOf("/"));
        if (direccionCarpeta == null) {
            logger.error("El video no tiene ruta");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("El video no tiene ruta");
        }

        Path carpetaPath = Paths.get(direccionCarpeta);

        Path videoPath = carpetaPath.resolve(lista);

        if (!Files.exists(videoPath)) {
            logger.error("No existe el archivo: {}", videoPath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe el archivo: " + videoPath);
        }

        // Obtener el tipo MIME del video
        String contentType = Files.probeContentType(videoPath);

        // Configurar las cabeceras de la respuesta
        HttpHeaders responseHeaders = this.createHeaders(contentType);

        long fileLength = Files.size(videoPath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fileLength)
                .headers(responseHeaders)
                .body(new InputStreamResource(Files.newInputStream(videoPath)));

    }

    /**
     * Función para obtener el video de una lista
     * 
     * @param idCurso ID del curso
     * @param idClase ID de la clase
     * @param lista   String con la lista a obtener
     * @param video   String con el nombre del video
     * @param headers HttpHeaders con las cabeceras del request
     * @return ResponseEntity<InputStreamResource> con el resultado de la operación
     * @throws IOException
     * @throws InternalServerException
     */
    @GetMapping("/{idCurso}/{idClase}/{lista}/{video}")
    public ResponseEntity<InputStreamResource> streamVideo(@PathVariable Long idCurso,
            @PathVariable Long idClase,
            @PathVariable String lista,
            @PathVariable String video,
            @RequestHeader HttpHeaders headers) throws IOException, InternalServerException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No autenticado");
        }
        Long idUsuario = (Long) authentication.getDetails();
        String direccionCarpeta = this.usuarioCursosService.getClase(idUsuario, idCurso, idClase);
        direccionCarpeta = direccionCarpeta.substring(0, direccionCarpeta.lastIndexOf("/"));
        if (direccionCarpeta == null) {
            logger.error("El video no tiene ruta");
            return ResponseEntity.notFound().build();
        }

        Path carpetaPath = Paths.get(direccionCarpeta);

        Path videoPath = carpetaPath.resolve(lista);

        videoPath = videoPath.resolve(video);

        if (!Files.exists(videoPath)) {
            logger.error("No existe el archivo: {}", videoPath);
            return ResponseEntity.notFound().build();
        }

        // Obtener el tipo MIME del video
        String contentType = Files.probeContentType(videoPath);

        // Configurar las cabeceras de la respuesta
        HttpHeaders responseHeaders = this.createHeaders(contentType);

        Long fileLength = Files.size(videoPath);
        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileLength)
                    .headers(responseHeaders)
                    .body(new InputStreamResource(Files.newInputStream(videoPath)));
        }

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(0);
        long end = range.getRangeEnd(fileLength - 1);
        if (end > fileLength - 1) {
            end = fileLength - 1;
        }
        long rangeLength = end - start + 1;

        RandomAccessFile file = new RandomAccessFile(videoPath.toFile(), "r");
        file.seek(start);

        InputStreamResource resource = new InputStreamResource(new LimitedInputStream(file, rangeLength));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .headers(responseHeaders)
                .body(resource);
    }

    /**
     * Función para rellenar la base de datos
     * 
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @GetMapping("/init")
    public ResponseEntity<?> get() {
        try {
            this.usuarioCursosService.syncUserCourses();
            return new ResponseEntity<>("Iniciado stream con exito!!!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para obtener el estado del curso
     * 
     * @param idCurso ID del curso
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @GetMapping("/status/{idCurso}")
    public ResponseEntity<?> getStatus(@PathVariable Long idCurso) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>("Error en el token: no autenticado", HttpStatus.UNAUTHORIZED);
            }
            Long idUsuario = (Long) authentication.getDetails();
            return new ResponseEntity<>(this.usuarioCursosService.getStatus(idUsuario, idCurso), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en obtener la clase: " + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para obtener la lista de la previsualización
     * 
     * @param idPreview String con el ID de la previsualización
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @GetMapping("/getPreview/{idPreview}")
    public ResponseEntity<?> getPreviewList(@PathVariable String idPreview) {
        try {
            Path previewPath = this.streamingService.getPreview(idPreview);
            if (previewPath == null || !Files.exists(previewPath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Obtener el tipo MIME del video
            String contentType = Files.probeContentType(previewPath);

            // Configurar las cabeceras de la respuesta
            HttpHeaders responseHeaders = this.createHeaders(contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(responseHeaders)
                    .body(new InputStreamResource(Files.newInputStream(previewPath)));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para obtener las partes de la previsualización
     * 
     * @param idPreview String con el ID de la previsualización
     * @param part      String con el nombre de la parte
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @GetMapping("/getPreview/{idPreview}/{part}")
    public ResponseEntity<?> getPreviewParts(@PathVariable String idPreview, @PathVariable String part) {
        try {
            Path previewPath = this.streamingService.getPreview(idPreview);
            if (previewPath == null || !Files.exists(previewPath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Path partPath = previewPath.getParent().resolve(idPreview).resolve(part);
            // Obtener el tipo MIME del video
            String contentType = Files.probeContentType(partPath);

            // Configurar las cabeceras de la respuesta
            HttpHeaders responseHeaders = this.createHeaders(contentType);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(responseHeaders)
                    .body(new InputStreamResource(Files.newInputStream(partPath)));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para añadir un usuario
     * 
     * @param usuario Usuario a añadir
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @PutMapping("/nuevoUsuario")
    public ResponseEntity<?> create(@RequestBody Usuario usuario) {
        try {
            return new ResponseEntity<>(this.usuarioCursosService.addNuevoUsuario(usuario), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para convertir los videos de un curso
     * 
     * @param curso Curso a convertir
     * @return ResponseEntity<String> con el resultado de la operación
     * 
     *         TODO: Modificar para trabajar solo con clases concretas
     * 
     */
    @PostMapping("/convertir_videos")
    public ResponseEntity<?> convertirVideos(@RequestBody Curso curso) {
        try {
            // StreamingService streamingService = new StreamingService(claseRepo);
            streamingService.convertVideos(curso);
            // ResponseEntity
            return new ResponseEntity<>("Videos convertidos con éxito!!!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para actualizar el streaming del curso
     * 
     * @param curso Curso a actualizar
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @PostMapping("/actualizar_curso_stream")
    public ResponseEntity<?> actualizarCursoStream(@RequestBody Curso curso) {
        try {
            this.usuarioCursosService.actualizarCursoStream(curso);
            return new ResponseEntity<>("Curso stream actualizado con éxito!!!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en actualizar el stream del curso: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para añadir una clase al curso
     * 
     * @param idCurso ID del curso
     * @param clase   Clase a añadir
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @PutMapping("/addClase/{idCurso}")
    public ResponseEntity<?> add(@PathVariable Long idCurso, @RequestBody Clase clase) {
        try {
            if (this.usuarioCursosService.addClase(idCurso, clase)) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para eliminar una clase del curso
     * 
     * @param idCurso ID del curso
     * @param idClase ID de la clase
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @DeleteMapping("/deleteClase/{idCurso}/{idClase}")
    public ResponseEntity<?> update(@PathVariable Long idCurso, @PathVariable Long idClase) {
        try {
            if (this.usuarioCursosService.deleteClase(idCurso, idClase)) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para eliminar el curso
     * 
     * @param id ID del curso
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @DeleteMapping("/deleteCurso/{id}")
    public ResponseEntity<?> deleteCurso(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(this.usuarioCursosService.deleteCurso(id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para eliminar el curso del usuario
     * 
     * @param id ID del usuario
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @DeleteMapping("/deleteUsuarioCursos/{id}")
    public ResponseEntity<?> deleteUsuarioCursos(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(this.usuarioCursosService.deleteUsuarioCursos(id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para crear las cabeceras de la respuesta
     * 
     * @param contentType String con el tipo de contenido
     * @return HttpHeaders con las cabeceras de la respuesta
     */
    protected HttpHeaders createHeaders(String contentType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType(contentType));
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
        responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-store");
        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        return responseHeaders;
    }
}
