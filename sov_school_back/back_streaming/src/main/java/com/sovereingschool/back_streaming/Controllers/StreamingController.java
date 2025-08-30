package com.sovereingschool.back_streaming.Controllers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
    }

    @Autowired
    private UsuarioCursosService usuarioCursosService;

    @Autowired
    private StreamingService streamingService;

    @GetMapping("/{id_curso}/{id_clase}/{lista}")
    public ResponseEntity<?> getListas(@PathVariable Long id_curso,
            @PathVariable Long id_clase,
            @PathVariable String lista,
            @RequestHeader HttpHeaders headers) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Error en el token de acceso", HttpStatus.UNAUTHORIZED);
        }
        Long id_usuario = (Long) authentication.getDetails();

        String direccion_carpeta = this.usuarioCursosService.getClase(id_usuario, id_curso, id_clase);
        if (direccion_carpeta == null) {
            System.err.println("No se encuentra la carpeta del curso: " + direccion_carpeta);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No se encuentra la carpeta del curso: " + direccion_carpeta);
        }
        direccion_carpeta = direccion_carpeta.substring(0, direccion_carpeta.lastIndexOf("/"));
        if (direccion_carpeta == null) {
            System.err.println("El video no tiene ruta");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("El video no tiene ruta");
        }

        Path carpetaPath = Paths.get(direccion_carpeta);

        Path videoPath = carpetaPath.resolve(lista);

        if (!Files.exists(videoPath)) {
            System.err.println("No existe el archivo: " + videoPath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe el archivo: " + videoPath);
        }

        // Obtener el tipo MIME del video
        String contentType = Files.probeContentType(videoPath);

        // Configurar las cabeceras de la respuesta
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType(contentType));
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
        responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-store");
        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        long fileLength = Files.size(videoPath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fileLength)
                .headers(responseHeaders)
                .body(new InputStreamResource(Files.newInputStream(videoPath)));

    }

    @GetMapping("/{id_curso}/{id_clase}/{lista}/{video}")
    public ResponseEntity<InputStreamResource> streamVideo(@PathVariable Long id_curso,
            @PathVariable Long id_clase,
            @PathVariable String lista,
            @PathVariable String video,
            @RequestHeader HttpHeaders headers) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No autenticado");
        }
        Long id_usuario = (Long) authentication.getDetails();
        String direccion_carpeta = this.usuarioCursosService.getClase(id_usuario, id_curso, id_clase);
        direccion_carpeta = direccion_carpeta.substring(0, direccion_carpeta.lastIndexOf("/"));
        if (direccion_carpeta == null) {
            System.err.println("El video no tiene ruta");
            return ResponseEntity.notFound().build();
        }

        Path carpetaPath = Paths.get(direccion_carpeta);

        Path videoPath = carpetaPath.resolve(lista);

        videoPath = videoPath.resolve(video);

        if (!Files.exists(videoPath)) {
            System.err.println("No existe el archivo: " + videoPath);
            return ResponseEntity.notFound().build();
        }

        // Obtener el tipo MIME del video
        String contentType = Files.probeContentType(videoPath);

        // Configurar las cabeceras de la respuesta
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType(contentType));
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
        responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-store");
        responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

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
     */
    @GetMapping("/init")
    public ResponseEntity<?> get() {
        try {
            this.usuarioCursosService.syncUserCourses();
            return new ResponseEntity<String>("Iniciado stream con exito!!!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/status/{id_curso}")
    public ResponseEntity<?> getStatus(@PathVariable Long id_curso) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>("Error en el token: no autenticado", HttpStatus.UNAUTHORIZED);
            }
            Long id_usuario = (Long) authentication.getDetails();
            return new ResponseEntity<Long>(this.usuarioCursosService.getStatus(id_usuario, id_curso), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en obtener la clase: " + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Obtener lista para la previsualización
    @GetMapping("/getPreview/{id_preview}")
    public ResponseEntity<?> getPreviewList(@PathVariable String id_preview) {
        try {
            Path previewPath = this.streamingService.getPreview(id_preview);
            if (previewPath == null || !Files.exists(previewPath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Obtener el tipo MIME del video
            String contentType = Files.probeContentType(previewPath);

            // Configurar las cabeceras de la respuesta
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType(contentType));
            responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
            responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-store");
            responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(responseHeaders)
                    .body(new InputStreamResource(Files.newInputStream(previewPath)));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Obtener partes de la previsualización
    @GetMapping("/getPreview/{id_preview}/{part}")
    public ResponseEntity<?> getPreviewParts(@PathVariable String id_preview, @PathVariable String part) {
        try {
            Path previewPath = this.streamingService.getPreview(id_preview);
            if (previewPath == null || !Files.exists(previewPath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Path partPath = previewPath.getParent().resolve(id_preview).resolve(part);
            // Obtener el tipo MIME del video
            String contentType = Files.probeContentType(partPath);

            // Configurar las cabeceras de la respuesta
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType(contentType));
            responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
            responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-store");
            responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(responseHeaders)
                    .body(new InputStreamResource(Files.newInputStream(partPath)));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/nuevoUsuario")
    public ResponseEntity<?> create(@RequestBody Usuario usuario) {
        try {
            return new ResponseEntity<>(this.usuarioCursosService.addNuevoUsuario(usuario), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/convertir_videos")
    public ResponseEntity<?> convertirVideos(@RequestBody Curso curso) {
        try {
            this.streamingService.convertVideos(curso);
            // ResponseEntity
            return new ResponseEntity<>("Videos convertidos con éxito!!!", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace(); // Usar un logger como SLF4J en entornos reales
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/addClase/{idCurso}")
    public ResponseEntity<?> add(@PathVariable Long idCurso, @RequestBody Clase clase) {
        try {
            if (this.usuarioCursosService.addClase(idCurso, clase)) {
                return new ResponseEntity<Boolean>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<Boolean>(false, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getCause().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/deleteClase/{idCurso}/{idClase}")
    public ResponseEntity<?> update(@PathVariable Long idCurso, @PathVariable Long idClase) {
        try {
            if (this.usuarioCursosService.deleteClase(idCurso, idClase)) {
                return new ResponseEntity<Boolean>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<Boolean>(false, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getCause().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/deleteCurso/{id}")
    public ResponseEntity<?> deleteCurso(@PathVariable Long id) {
        try {

            return new ResponseEntity<>(this.usuarioCursosService.deleteCurso(id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/actualizar_curso_stream")
    public ResponseEntity<?> actualizarCursoStream(@RequestBody Curso curso) {
        try {
            this.usuarioCursosService.actualizarCursoStream(curso);
            return new ResponseEntity<String>("Curso stream actualizado con éxito!!!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en actualizar el stream del curso: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
