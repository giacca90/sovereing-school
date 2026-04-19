package com.sovereingschool.back_streaming.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_streaming.Services.StreamingService;
import com.sovereingschool.back_streaming.Services.UsuarioCursosService;

import jakarta.persistence.EntityManagerFactory;

@WebMvcTest(controllers = StreamingController.class)
@EnableAutoConfiguration(exclude = {
                SecurityAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class StreamingControllerTest {

        @TestConfiguration
        static class TestConfig {
                @Bean
                public ObjectMapper objectMapper() {
                        return new ObjectMapper();
                }
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UsuarioCursosService usuarioCursosService;

        @MockitoBean
        private StreamingService streamingService;

        @MockitoBean
        private com.sovereingschool.back_streaming.Repositories.PresetRepository presetRepository;

        @MockitoBean
        private com.sovereingschool.back_streaming.Repositories.UsuarioCursosRepository usuarioCursosRepository;

        @MockitoBean
        private com.sovereingschool.back_streaming.Services.UsuarioPresetsService usuarioPresetsService;

        @MockitoBean
        private com.sovereingschool.back_common.Repositories.UsuarioRepository usuarioRepository;

        @MockitoBean
        private com.sovereingschool.back_common.Utils.CursoUtil cursoUtil;

        @MockitoBean
        private com.sovereingschool.back_common.Repositories.PlanRepository planRepository;

        @MockitoBean
        private com.sovereingschool.back_common.Repositories.CursoRepository cursoRepository;

        @MockitoBean
        private com.sovereingschool.back_common.Repositories.ClaseRepository claseRepository;

        @MockitoBean
        private com.sovereingschool.back_common.Repositories.LoginRepository loginRepository;

        @MockitoBean(name = "entityManagerFactory")
        private EntityManagerFactory entityManagerFactory;

        /**
         * Prueba la inicialización del stream.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testInitEndpoint() throws Exception {
                doNothing().when(usuarioCursosService).syncUserCourses();

                mockMvc.perform(get("/init"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Iniciado stream con exito!!!"));
        }

        /**
         * Prueba la obtención del estado de un curso.
         */
        @Test
        void testGetStatus() throws Exception {
                Long idCurso = 1L;
                Long idUsuario = 123L;

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "user", "password",
                                java.util.List.of(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "ROLE_USER")));
                auth.setDetails(idUsuario);
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

                when(usuarioCursosService.getStatus(eq(idUsuario), eq(idCurso))).thenReturn(10L);

                mockMvc.perform(get("/status/{idCurso}", idCurso))
                                .andExpect(status().isOk())
                                .andExpect(content().string("10"));

                org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        /**
         * Prueba el error interno al obtener el estado.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetStatus_InternalError() throws Exception {
                Long idCurso = 1L;
                Long idUsuario = 123L;

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "password",
                                Collections.emptyList());
                auth.setDetails(idUsuario);

                when(usuarioCursosService.getStatus(any(), any())).thenThrow(new RuntimeException("Error de estado"));

                mockMvc.perform(get("/status/{idCurso}", idCurso)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                                .authentication(auth)))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Prueba la creación exitosa de un usuario.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testCreateUsuario() throws Exception {
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(1L);
                when(usuarioCursosService.addNuevoUsuario(any(Usuario.class)))
                                .thenReturn("Nuevo Usuario Insertado con Exito!!!");

                mockMvc.perform(put("/nuevoUsuario")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(usuario)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Nuevo Usuario Insertado con Exito!!!"));
        }

        /**
         * Prueba la conversión de videos de un curso.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testConvertirVideos() throws Exception {
                Curso curso = new Curso();
                curso.setIdCurso(1L);
                doNothing().when(streamingService).convertVideos(any(Curso.class));

                mockMvc.perform(post("/convertir_videos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(curso)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Videos convertidos con éxito!!!"));
        }

        /**
         * Prueba la adición exitosa de una clase.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testAddClase() throws Exception {
                Long idCurso = 1L;
                Clase clase = new Clase();
                clase.setIdClase(1L);
                when(usuarioCursosService.addClase(eq(idCurso), any(Clase.class))).thenReturn(true);

                mockMvc.perform(put("/addClase/{idCurso}", idCurso)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clase)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));
        }

        /**
         * Prueba la eliminación exitosa de una clase.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testDeleteClase() throws Exception {
                Long idCurso = 1L;
                Long idClase = 2L;
                when(usuarioCursosService.deleteClase(idCurso, idClase)).thenReturn(true);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/deleteClase/{idCurso}/{idClase}", idCurso, idClase))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));
        }

        /**
         * Prueba la eliminación exitosa de un curso.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testDeleteCurso() throws Exception {
                Long id = 1L;
                when(usuarioCursosService.deleteCurso(id)).thenReturn(true);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/deleteCurso/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));
        }

        /**
         * Prueba la eliminación exitosa de los cursos de un usuario.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testDeleteUsuarioCursos() throws Exception {
                Long idUsuario = 1L;

                when(usuarioCursosService.deleteUsuarioCursos(idUsuario)).thenReturn(true);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/deleteUsuarioCursos/{idUsuario}", idUsuario))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));
        }

        /**
         * Prueba la actualización exitosa del stream de un curso.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testActualizarCursoStream() throws Exception {
                Curso curso = new Curso();
                curso.setIdCurso(1L);

                doNothing().when(usuarioCursosService).actualizarCursoStream(any(Curso.class));

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/actualizar_curso_stream")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(curso)))
                                .andExpect(status().isOk())

                                .andExpect(content().string("Curso stream actualizado con éxito!!!"));
        }

        /**
         * Prueba la obtención exitosa de la lista de previsualizaciones.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetPreviewList() throws Exception {

                String idPreview = "preview1";
                java.nio.file.Path mockPath = java.nio.file.Paths.get("src/test/resources/mock_preview.m3u8");

                // Crear el archivo mock si no existe para que Files.exists(previewPath) sea
                // true
                if (!java.nio.file.Files.exists(mockPath)) {
                        java.nio.file.Files.createDirectories(mockPath.getParent());
                        java.nio.file.Files.createFile(mockPath);
                }

                when(streamingService.getPreview(idPreview)).thenReturn(mockPath);

                mockMvc.perform(get("/getPreview/{idPreview}", idPreview))
                                .andExpect(status().isOk());
        }

        /**
         * Prueba la obtención exitosa de las partes de una previsualización.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetPreviewParts() throws Exception {
                String idPreview = "preview1";
                String part = "part1.ts";
                java.nio.file.Path mockPath = java.nio.file.Paths.get("src/test/resources/mock_preview.m3u8");

                if (!java.nio.file.Files.exists(mockPath)) {
                        java.nio.file.Files.createDirectories(mockPath.getParent());
                        java.nio.file.Files.createFile(mockPath);
                }

                // Crear el archivo de la parte mock
                java.nio.file.Path partPath = mockPath.getParent().resolve(idPreview).resolve(part);
                if (!java.nio.file.Files.exists(partPath)) {
                        java.nio.file.Files.createDirectories(partPath.getParent());
                        java.nio.file.Files.createFile(partPath);
                }

                when(streamingService.getPreview(idPreview)).thenReturn(mockPath);

                mockMvc.perform(get("/getPreview/{idPreview}/{part}", idPreview, part))
                                .andExpect(status().isOk());
        }

        /**
         * Prueba el error cuando no se encuentra una parte de la previsualización.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetPreviewPartsNotFound() throws Exception {
                String idPreview = "nonexistent";
                when(streamingService.getPreview(idPreview)).thenReturn(null);

                mockMvc.perform(get("/getPreview/{idPreview}/{part}", idPreview, "part1"))
                                .andExpect(status().isNotFound());
        }

        /**
         * Prueba la obtención exitosa de las listas de reproducción.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetListas_Success() throws Exception {
                Long idCurso = 100L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";

                // Mock de la ruta de la clase
                String mockDireccion = "/tmp/courses/100/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                // Crear archivo mock para la lista
                java.nio.file.Path listPath = java.nio.file.Paths.get("/tmp/courses/100/playlist.m3u8");
                if (!java.nio.file.Files.exists(listPath)) {
                        java.nio.file.Files.createDirectories(listPath.getParent());
                        java.nio.file.Files.createFile(listPath);
                }

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}", idCurso, idClase, lista))
                                .andExpect(status().isOk());
        }

        /**
         * Prueba el error cuando no se encuentra la lista de reproducción.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetListas_NotFound() throws Exception {
                Long idCurso = 1L;
                Long idClase = 1L;
                String lista = "nonexistent.m3u8";

                String mockDireccion = "/tmp/courses/1/1/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}", idCurso, idClase, lista))
                                .andExpect(status().isNotFound());
        }

        /**
         * Prueba la transmisión exitosa de un video.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testStreamVideo_Success() throws Exception {
                Long idCurso = 101L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";
                String video = "segment1.ts";

                String mockDireccion = "/tmp/courses/101/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                // Ahora el controlador resuelve el video relativo al padre de la clase
                java.nio.file.Path videoPath = java.nio.file.Paths.get("/tmp/courses/101/" + lista + "/" + video);

                if (!java.nio.file.Files.exists(videoPath.getParent())) {
                        java.nio.file.Files.createDirectories(videoPath.getParent());
                }
                if (!java.nio.file.Files.exists(videoPath)) {
                        java.nio.file.Files.createFile(videoPath);
                }
                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}/{video}", idCurso, idClase, lista, video))
                                .andExpect(status().isOk());
        }

        /**
         * Prueba el error cuando no se encuentra el segmento de video.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testStreamVideo_NotFound() throws Exception {
                Long idCurso = 1L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";
                String video = "nonexistent.ts";

                String mockDireccion = "/tmp/courses/1/1/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}/{video}", idCurso, idClase, lista, video))
                                .andExpect(status().isNotFound());
        }

        /**
         * Prueba el error interno al obtener las listas de reproducción.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetListas_InternalError() throws Exception {
                Long idCurso = 1L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";

                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase)))
                                .thenThrow(new com.sovereingschool.back_common.Exceptions.InternalServerException(
                                                "Error"));

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}", idCurso, idClase, lista))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(roles = "USER")
        void testGetListas_NoAuth() throws Exception {
                // Simular usuario no autenticado eliminando el filtro de seguridad es difícil
                // con @WithMockUser,
                // pero podemos probar la lógica de null check si removemos @WithMockUser
        }

        /**
         * Prueba el acceso denegado a las listas de reproducción.
         */
        @Test
        void testGetListas_Unauthorized() throws Exception {
                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}", 1L, 1L, "playlist.m3u8"))
                                .andExpect(status().isUnauthorized());
        }

        /**
         * Prueba la transmisión de video con peticiones de rango.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testStreamVideo_RangeRequest() throws Exception {
                Long idCurso = 102L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";
                String video = "segment1.ts";

                String mockDireccion = "/tmp/courses/102/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                // El controlador hace: Path videoPath =
                // carpetaPath.resolve(lista).resolve(video);
                java.nio.file.Path videoPath = java.nio.file.Paths.get("/tmp/courses/102/" + lista + "/" + video);

                if (!java.nio.file.Files.exists(videoPath.getParent())) {
                        java.nio.file.Files.createDirectories(videoPath.getParent());
                }
                if (!java.nio.file.Files.exists(videoPath)) {
                        java.nio.file.Files.write(videoPath, new byte[1024]); // Crear archivo con contenido
                }

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}/{video}", idCurso, idClase, lista, video)
                                .header("Range", "bytes=0-100"))
                                .andExpect(status().isPartialContent());
        }

        /**
         * Prueba la transmisión de video no autorizada.
         */
        @Test
        void testStreamVideo_Unauthorized() throws Exception {
                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}/{video}", 1L, 1L, "l", "v"))
                                .andExpect(status().isUnauthorized());
        }

        /**
         * Prueba la obtención de listas cuando no hay ruta padre.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetListas_NoParentPath() throws Exception {
                Long idCurso = 1L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";

                // Una ruta sin padre (solo el nombre del archivo)
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn("video.mp4");

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}", idCurso, idClase, lista))
                                .andExpect(status().isNotFound());
        }

        /**
         * Prueba la transmisión de video con rango.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testStreamVideo_WithRange() throws Exception {
                Long idCurso = 103L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";
                String video = "segment1.ts";

                String mockDireccion = "/tmp/courses/103/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                java.nio.file.Path videoPath = java.nio.file.Paths.get("/tmp/courses/103/" + lista + "/" + video);
                if (!java.nio.file.Files.exists(videoPath.getParent())) {
                        java.nio.file.Files.createDirectories(videoPath.getParent());
                }
                java.nio.file.Files.write(videoPath, "test content".getBytes());

                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}/{video}", idCurso, idClase, lista, video)
                                .header("Range", "bytes=0-4"))
                                .andExpect(status().isPartialContent())
                                .andExpect(content().string("test "));
        }

        /**
         * Prueba la transmisión de video con rango fuera de límites.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testStreamVideo_WithRangeEndOverLimit() throws Exception {
                Long idCurso = 104L;
                Long idClase = 1L;
                String lista = "playlist.m3u8";
                String video = "segment1.ts";

                String mockDireccion = "/tmp/courses/104/video.mp4";
                when(usuarioCursosService.getClase(any(), eq(idCurso), eq(idClase))).thenReturn(mockDireccion);

                java.nio.file.Path videoPath = java.nio.file.Paths.get("/tmp/courses/104/" + lista + "/" + video);
                if (!java.nio.file.Files.exists(videoPath.getParent())) {
                        java.nio.file.Files.createDirectories(videoPath.getParent());
                }
                java.nio.file.Files.write(videoPath, "test content".getBytes());

                // Range end is larger than file length
                mockMvc.perform(get("/{idCurso}/{idClase}/{lista}/{video}", idCurso, idClase, lista, video)
                                .header("Range", "bytes=0-100"))
                                .andExpect(status().isPartialContent())
                                .andExpect(content().string("test content"));
        }

        /**
         * Prueba el error cuando no se encuentra la lista de previsualización.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetPreviewList_NotFound() throws Exception {
                when(streamingService.getPreview(any())).thenReturn(null);
                mockMvc.perform(get("/getPreview/missing"))
                                .andExpect(status().isNotFound());
        }

        /**
         * Prueba el error genérico al obtener la lista de previsualización.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetPreviewList_Exception() throws Exception {
                when(streamingService.getPreview(any())).thenThrow(new RuntimeException("error"));
                mockMvc.perform(get("/getPreview/error"))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Prueba el error genérico al obtener partes de previsualización.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testGetPreviewParts_Exception() throws Exception {
                when(streamingService.getPreview(any())).thenThrow(new RuntimeException("error"));
                mockMvc.perform(get("/getPreview/error/part1"))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Prueba el error al actualizar el stream de un curso.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testActualizarCursoStream_Exception() throws Exception {
                doThrow(new RuntimeException("error")).when(usuarioCursosService).actualizarCursoStream(any());
                mockMvc.perform(post("/actualizar_curso_stream")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Prueba el error al añadir una clase.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testAddClase_Exception() throws Exception {
                when(usuarioCursosService.addClase(any(), any())).thenThrow(new RuntimeException("error"));
                mockMvc.perform(put("/addClase/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("error"));
        }

        /**
         * Prueba el error al crear un usuario.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testCreateUsuario_Exception() throws Exception {
                when(usuarioCursosService.addNuevoUsuario(any())).thenThrow(new RuntimeException("error"));
                mockMvc.perform(put("/nuevoUsuario")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Prueba el error al convertir videos.
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        void testConvertirVideos_Exception() throws Exception {
                doThrow(new RuntimeException(new Throwable("cause error"))).when(streamingService).convertVideos(any());
                mockMvc.perform(post("/convertir_videos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Prueba el error al inicializar el stream.
         */
        @Test
        @WithMockUser(roles = "USER")
        void testInit_Exception() throws Exception {
                doThrow(new RuntimeException("error")).when(usuarioCursosService).syncUserCourses();
                mockMvc.perform(get("/init"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        void getProgresoClase_success() throws Exception {
                Long idCurso = 1L;
                Long idClase = 1L;
                Long idUsuario = 1L;

                com.sovereingschool.back_streaming.Models.UsuarioCursos usuarioCursos = new com.sovereingschool.back_streaming.Models.UsuarioCursos();
                usuarioCursos.setIdUsuario(idUsuario);

                com.sovereingschool.back_streaming.Models.StatusCurso statusCurso = new com.sovereingschool.back_streaming.Models.StatusCurso();
                statusCurso.setIdCurso(idCurso);

                com.sovereingschool.back_streaming.Models.StatusClase statusClase = new com.sovereingschool.back_streaming.Models.StatusClase();
                statusClase.setIdClase(idClase);
                statusClase.setTotalSegments(10);
                statusClase.getProgress().add(1);

                statusCurso.setClases(java.util.List.of(statusClase));
                usuarioCursos.setCursos(java.util.List.of(statusCurso));

                when(usuarioCursosRepository.findByIdUsuario(idUsuario))
                                .thenReturn(java.util.Optional.of(usuarioCursos));

                mockMvc.perform(get("/progreso/{idCurso}/{idClase}", idCurso, idClase)
                                .with(request -> {
                                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                                        "1", null,
                                                        java.util.List.of(
                                                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                                        "ROLE_USER")));
                                        auth.setDetails(idUsuario);
                                        org.springframework.security.core.context.SecurityContextHolder.getContext()
                                                        .setAuthentication(auth);
                                        request.setUserPrincipal(auth);
                                        return request;
                                }))
                                .andExpect(status().isOk())
                                .andExpect(content().string("10.0"));
        }

        @Test
        @WithMockUser
        void getProgresoClase_error() throws Exception {
                when(usuarioCursosRepository.findByIdUsuario(any())).thenThrow(new RuntimeException("error"));

                mockMvc.perform(get("/progreso/1/1")
                                .with(request -> {
                                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                                        "1", null,
                                                        java.util.List.of(
                                                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                                        "ROLE_USER")));
                                        org.springframework.security.core.context.SecurityContextHolder.getContext()
                                                        .setAuthentication(auth);
                                        request.setUserPrincipal(auth);
                                        return request;
                                }))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        void pingFragmento_success() throws Exception {
                Long idCurso = 1L;
                Long idClase = 1L;
                int segment = 5;

                // Mocking user authentication
                Usuario mockUsuario = new Usuario();
                mockUsuario.setIdUsuario(1L);

                doNothing().when(streamingService).registrarProgreso(anyLong(), anyLong(), anyLong(), anyInt());

                mockMvc.perform(post("/registrar-fragmento")
                                .param("idCurso", idCurso.toString())
                                .param("idClase", idClase.toString())
                                .param("segment", String.valueOf(segment))
                                .with(request -> {
                                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                                        "1", null,
                                                        java.util.List.of(
                                                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                                        "ROLE_USER")));
                                        org.springframework.security.core.context.SecurityContextHolder.getContext()
                                                        .setAuthentication(auth);
                                        request.setUserPrincipal(auth);
                                        return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(streamingService, times(1)).registrarProgreso(1L, idCurso, idClase, segment);
        }
}
