package com.sovereingschool.back_base.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
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
import com.sovereingschool.back_common.Repositories.PlanRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    // ==========================
    // Tests createCurso()
    // ==========================
    @Nested
    class CreateCursoTests {

        private Curso curso;

        @BeforeEach
        void setUp() {
            curso = new Curso();
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
        }

        @Test
        void createCurso_SuccessfulCreation() throws RepositoryException {
            // Simular que el repo asigna un ID
            Curso savedCurso = new Curso();
            savedCurso.setIdCurso(10L);
            savedCurso.setNombreCurso("curso");
            savedCurso.setDescripcionCorta("descripcion corta");
            savedCurso.setFechaPublicacionCurso(curso.getFechaPublicacionCurso());

            when(cursoRepo.save(curso)).thenReturn(savedCurso);

            Long resp = cursoService.createCurso(curso);

            assertNotNull(resp);
            assertEquals(10L, resp);

            verify(cursoRepo).save(curso);
        }

        @Test
        void createCurso_ThrowsRepositoryException_WhenSavedCursoHasNullId() {
            when(cursoRepo.save(curso)).thenThrow(new IllegalArgumentException());

            RepositoryException ex = assertThrows(RepositoryException.class,
                    () -> cursoService.createCurso(curso));

            assertTrue(ex.getMessage().contains("Error en crear el curso"));
            verify(cursoRepo).save(curso);
        }
    }

    // ==========================
    // Tests getCurso()
    // ==========================
    @Nested
    class GetCursoTests {

        private Long cursoId;
        private Curso curso;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            curso = new Curso();
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
        }

        @Test
        void getCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));

            Curso resp = cursoService.getCurso(cursoId);

            assertNotNull(resp);
            assertEquals(curso.getNombreCurso(), resp.getNombreCurso());
            assertEquals(curso.getDescripcionCorta(), resp.getDescripcionCorta());
            assertEquals(curso.getFechaPublicacionCurso(), resp.getFechaPublicacionCurso());

            verify(cursoRepo).findById(cursoId);
        }

        @Test
        void getCurso_Error() {
            // Simular que el curso no existe
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getCurso(cursoId));

            assertEquals("Error en obtener el curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findById(cursoId);
        }
    }

    // ==========================
    // Tests getNombreCurso()
    // ==========================
    @Nested
    class GetNombreCursoTests {

        private Long cursoId;
        private String nombreCurso;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            nombreCurso = "curso";
        }

        @Test
        void getNombreCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findNombreCursoById(cursoId)).thenReturn(Optional.of(nombreCurso));

            String resp = cursoService.getNombreCurso(cursoId);

            assertNotNull(resp);
            assertEquals(nombreCurso, resp);

            verify(cursoRepo).findNombreCursoById(cursoId);
        }

        @Test
        void getNombreCurso_Error() {
            // Simular que el curso no existe
            when(cursoRepo.findNombreCursoById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getNombreCurso(cursoId));

            assertEquals("Error en obtener el nombre del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findNombreCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getProfesoresCurso()
    // ==========================
    @Nested
    class GetProfesoresCursoTests {

        private Long cursoId;
        private List<Usuario> profesores;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            profesores = List.of(new Usuario(), new Usuario());
        }

        @Test
        void getProfesoresCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findProfesoresCursoById(cursoId)).thenReturn(profesores);

            List<Usuario> resp = cursoService.getProfesoresCurso(cursoId);

            assertNotNull(resp);
            assertEquals(profesores, resp);

            verify(cursoRepo).findProfesoresCursoById(cursoId);
        }

        @Test
        void getProfesoresCurso_ErrorEmptyList() {
            // Simular que la lista está vacía
            when(cursoRepo.findProfesoresCursoById(cursoId)).thenReturn(List.of());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getProfesoresCurso(cursoId));

            assertEquals("Error en obtener los profesores del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findProfesoresCursoById(cursoId);
        }

        @Test
        void getProfesoresCurso_ErrorNullList() {
            // Simular que la lista está vacía
            when(cursoRepo.findProfesoresCursoById(cursoId)).thenReturn(null);

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getProfesoresCurso(cursoId));

            assertEquals("Error en obtener los profesores del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findProfesoresCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getFechaCreacionCurso()
    // ==========================
    @Nested
    class GetFechaCreacionCursoTests {

        private Long cursoId;
        private Date fechaCreacionCurso;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            fechaCreacionCurso = new Date();
        }

        @Test
        void getFechaCreacionCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findFechaCreacionCursoById(cursoId)).thenReturn(Optional.of(fechaCreacionCurso));

            Date resp = cursoService.getFechaCreacionCurso(cursoId);

            assertNotNull(resp);
            assertEquals(fechaCreacionCurso, resp);

            verify(cursoRepo).findFechaCreacionCursoById(cursoId);
        }

        @Test
        void getFechaCreacionCurso_Error() {
            // Simular que el curso no existe
            when(cursoRepo.findFechaCreacionCursoById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getFechaCreacionCurso(cursoId));

            assertEquals("Error en obtener la fecha de creación del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findFechaCreacionCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getClasesDelCurso()
    // ==========================
    @Nested
    class GetClasesDelCursoTests {

        private Long cursoId;
        private List<Clase> clases;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            clases = List.of(new Clase(), new Clase());
        }

        @Test
        void getClasesDelCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findClasesCursoById(cursoId)).thenReturn(clases);

            List<Clase> resp = cursoService.getClasesDelCurso(cursoId);

            assertNotNull(resp);
            assertEquals(clases, resp);

            verify(cursoRepo).findClasesCursoById(cursoId);
        }

        @Test
        void getClasesDelCurso_ErrorEmptyList() {
            // Simular que la lista está vacía
            when(cursoRepo.findClasesCursoById(cursoId)).thenReturn(List.of());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getClasesDelCurso(cursoId));

            assertEquals("Error en obtener las clases del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findClasesCursoById(cursoId);
        }

        @Test
        void getClasesDelCurso_ErrorNullList() {
            // Simular que la lista está vacía
            when(cursoRepo.findClasesCursoById(cursoId)).thenReturn(null);

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getClasesDelCurso(cursoId));

            assertEquals("Error en obtener las clases del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findClasesCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getPlanesDelCurso()
    // ==========================
    @Nested
    class GetPlanesDelCursoTests {

        private Long cursoId;
        private List<Plan> planes;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            planes = List.of(new Plan(), new Plan());
        }

        @Test
        void getPlanesDelCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findPlanesCursoById(cursoId)).thenReturn(planes);

            List<Plan> resp = cursoService.getPlanesDelCurso(cursoId);

            assertNotNull(resp);
            assertEquals(planes, resp);

            verify(cursoRepo).findPlanesCursoById(cursoId);
        }

        @Test
        void getPlanesDelCurso_ErrorEmptyList() {
            // Simular que la lista está vacía
            when(cursoRepo.findPlanesCursoById(cursoId)).thenReturn(List.of());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getPlanesDelCurso(cursoId));

            assertEquals("Error en obtener los planes del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findPlanesCursoById(cursoId);
        }

        @Test
        void getPlanesDelCurso_ErrorNullList() {
            // Simular que la lista está vacía
            when(cursoRepo.findPlanesCursoById(cursoId)).thenReturn(null);

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getPlanesDelCurso(cursoId));

            assertEquals("Error en obtener los planes del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findPlanesCursoById(cursoId);
        }
    }

    // ==========================
    // Tests getPrecioCurso()
    // ==========================
    @Nested
    class GetPrecioCursoTests {

        private Long cursoId;
        private BigDecimal precioCurso;

        @BeforeEach
        void setUp() {
            cursoId = 1L;
            precioCurso = new BigDecimal(100);
        }

        @Test
        void getPrecioCurso_SuccessfulRetrieval() throws NotFoundException {
            when(cursoRepo.findPrecioCursoById(cursoId)).thenReturn(Optional.of(precioCurso));

            BigDecimal resp = cursoService.getPrecioCurso(cursoId);

            assertNotNull(resp);
            assertEquals(precioCurso, resp);

            verify(cursoRepo).findPrecioCursoById(cursoId);
        }

        @Test
        void getPrecioCurso_Error() {
            // Simular que el curso no existe
            when(cursoRepo.findPrecioCursoById(cursoId)).thenReturn(Optional.empty());

            // Verificar que lanza la excepción esperada
            NotFoundException thrown = assertThrows(NotFoundException.class,
                    () -> cursoService.getPrecioCurso(cursoId));

            assertEquals("Error en obtener el precio del curso con ID " + cursoId, thrown.getMessage());

            verify(cursoRepo).findPrecioCursoById(cursoId);
        }
    }

    // ==========================
    // Tests updateCurso()
    // ==========================
    @Nested
    class UpdateCursoTests {

        private Curso curso;
        private Curso cursoGuardado;
        private Clase clase1;
        private Clase clase2;
        private List<Clase> clases;
        private Clase claseGuardada1;
        private Clase claseGuardada2;
        private List<Clase> clasesGuardadas;

        @BeforeEach
        void setUp() {
            clase1 = new Clase();
            clase1.setIdClase(0L);
            clase1.setNombreClase("clase1");
            clase1.setPosicionClase(1);
            clase2 = new Clase();
            clase2.setIdClase(0L);
            clase2.setNombreClase("clase2");
            clase2.setPosicionClase(2);
            clases = List.of(clase1, clase2);
            claseGuardada1 = new Clase();
            claseGuardada1.setIdClase(1L);
            claseGuardada2 = new Clase();
            claseGuardada2.setIdClase(2L);
            clasesGuardadas = List.of(claseGuardada1, claseGuardada2);

            curso = new Curso();
            curso.setIdCurso(0L);
            curso.setNombreCurso("curso");
            curso.setDescripcionCorta("descripcion corta");
            curso.setFechaPublicacionCurso(new Date());
            curso.setClasesCurso(clases);

            cursoGuardado = new Curso();
            cursoGuardado.setIdCurso(1l);
            cursoGuardado.setNombreCurso("curso");
            cursoGuardado.setDescripcionCorta("descripcion corta");
            cursoGuardado.setFechaPublicacionCurso(new Date());
            cursoGuardado.setClasesCurso(clasesGuardadas);

            // Mocks para claseRepo.save
            lenient().when(claseRepo.save(clase1)).thenReturn(claseGuardada1);
            lenient().when(claseRepo.save(clase2)).thenReturn(claseGuardada2);
        }

        @Test
        void updateCurso_NewCurso_SuccessfulUpdate()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {

            // 1. CONFIGURACIÓN DE MOCKS PARA LLAMADAS ASÍNCRONAS (WebClient)
            lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

            // Configuramos respuestas secuenciales para bodyToMono
            // La primera llamada recibirá la del chat, la segunda la de stream
            lenient().when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.just("Curso chat actualizado con éxito!!!")) // 1ª invocación
                    .thenReturn(Mono.just("Curso stream actualizado con éxito!!!")); // 2ª invocación

            // 2. PREPARACIÓN DEL ESCENARIO
            CursoService spyService = spy(cursoService);

            // Mocks de repositorio
            when(cursoRepo.save(any(Curso.class))).thenReturn(cursoGuardado);
            when(claseRepo.save(clase1)).thenReturn(claseGuardada1);
            when(claseRepo.save(clase2)).thenReturn(claseGuardada2);

            // 3. EJECUCIÓN
            Curso resp = spyService.updateCurso(curso);

            // 4. ASERCIONES Y VERIFICACIONES
            assertNotNull(resp);
            assertEquals(cursoGuardado.getIdCurso(), resp.getIdCurso());

            // Verificamos que los métodos del repositorio fueron llamados
            // times(2) es correcto: uno para el save inicial y otro dentro de
            // creaClasesCurso
            verify(cursoRepo, times(2)).save(any(Curso.class));

            // Verificamos llamadas a métodos internos
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();

            // Verificación técnica de la cadena WebClient
            // Esperamos 2 posts: chat, stream
            verify(webClient, times(2)).post();
            verify(responseSpec, times(2)).bodyToMono(String.class);
        }

        @Test
        void updateCurso_OldCurso_SuccessfulUpdate()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException,
                IOException {
            Long cursoId = 10L;
            curso.setIdCurso(cursoId);
            clase1.setIdClase(25L);
            clase2.setIdClase(null);

            Path path = tempDir.resolve(cursoId.toString());
            Files.createDirectory(path);

            // 1. CONFIGURACIÓN DE MOCKS PARA LLAMADAS ASÍNCRONAS (WebClient)
            lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

            // Configuramos respuestas secuenciales para bodyToMono
            // La primera llamada recibirá la del chat, la segunda la de stream
            lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("ERROR!!!"));

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(curso);
            doNothing().when(spyService).creaCarpetaCurso(curso);

            // Act
            Curso resp = spyService.updateCurso(curso);

            // Assert
            assertNotNull(resp);
            assertEquals(curso, resp);
            verify(spyService).creaCarpetaCurso(curso);
            verify(spyService).creaClasesCurso(curso, clases);
            verify(spyService).actualizarChatCurso(curso);
            verify(spyService).actualizarStreamCurso(curso);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_NewCurso_SuccessfulUpdate_NoClases()
                throws InternalServerException, InternalComunicationException,
                RepositoryException, NotFoundException, IOException {
            curso.setClasesCurso(null);

            Path path = tempDir.resolve(curso.getIdCurso().toString());
            Files.createDirectory(path);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);

            // Act
            Curso resp = spyService.updateCurso(curso);

            // Assert
            assertNotNull(resp);
            assertEquals(cursoGuardado, resp);
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, null);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_NewCurso_SuccessfulUpdate_EmptyClases()
                throws InternalServerException, InternalComunicationException,
                RepositoryException, NotFoundException, IOException {
            curso.setClasesCurso(new ArrayList<>());

            Path path = tempDir.resolve(curso.getIdCurso().toString());
            Files.createDirectory(path);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);

            // Act
            Curso resp = spyService.updateCurso(curso);

            // Assert
            assertNotNull(resp);
            assertEquals(cursoGuardado, resp);
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, new ArrayList<>());
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_OldCurso_ErrorGuardarCurso() throws InternalServerException {
            Long cursoId = 10L;
            curso.setIdCurso(cursoId);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenThrow(new IllegalArgumentException("save error"));

            doNothing().when(spyService).creaCarpetaCurso(curso);

            // Act
            RepositoryException ex = assertThrows(RepositoryException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("save error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(curso);

        }

        @Test
        void updateCurso_NewCurso_ErrorCrearCarpeta()
                throws InternalServerException {
            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            tempDir.toFile().setWritable(false);

            // Act
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error en crear la carpeta del curso."));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
        }

        @Test
        void updateCurso_NewCurso_ErrorCrearClasesCurso()
                throws InternalServerException, RepositoryException {
            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            doNothing().when(spyService).creaCarpetaCurso(cursoGuardado);
            doThrow(new InternalServerException("Error")).when(spyService).creaClasesCurso(cursoGuardado, clases);

            // Act
            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
        }

        @Test
        void updateCurso_NewCurso_ErrorConexionEntreMicroservicio()
                throws InternalServerException, InternalComunicationException, RepositoryException, IOException,
                NotFoundException {

            // 1. CONFIGURACIÓN PARA FORZAR LA ENTRADA EN EL FLATMAP (onStatus)
            String errorBody = "Internal Server Error";

            // Creamos un mock de la respuesta que recibiría el onStatus
            org.springframework.web.reactive.function.client.ClientResponse mockResponse = mock(
                    org.springframework.web.reactive.function.client.ClientResponse.class);
            lenient().when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just(errorBody));

            // Interceptamos onStatus para ejecutar el flatMap manualmente
            lenient().when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
                // El argumento 1 es la lambda: response ->
                // response.bodyToMono(...).flatMap(...)
                java.util.function.Function<org.springframework.web.reactive.function.client.ClientResponse, Mono<? extends Throwable>> flatMapFunction = invocation
                        .getArgument(1);

                // Ejecutamos la lambda. Esto es lo que da cobertura al flatMap y al Mono.error
                flatMapFunction.apply(mockResponse).subscribe();

                return responseSpec; // Devolvemos el spec para seguir la cadena fluida
            });

            // Configuramos bodyToMono para que emita el error y dispare el .onErrorResume()
            lenient().when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.error(new InternalComunicationException("Error del microservicio: " + errorBody)));

            // 2. PREPARACIÓN DEL ESCENARIO
            CursoService spyService = spy(cursoService);

            Path path = tempDir.resolve(cursoGuardado.getIdCurso().toString());
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }

            when(cursoRepo.save(any(Curso.class))).thenReturn(cursoGuardado);
            lenient().when(claseRepo.save(any())).thenReturn(claseGuardada1);

            // 3. EJECUCIÓN
            Curso resp = spyService.updateCurso(curso);

            // 4. ASERCIONES Y VERIFICACIONES
            assertNotNull(resp);

            // Verificamos que se ejecutaron las 3 llamadas asíncronas (Chat, Stream, SSR)
            verify(spyService).actualizarChatCurso(any());
            verify(spyService).actualizarStreamCurso(any());
            verify(spyService).updateSSR();

            // Verificamos que el WebClient hizo los 3 POSTs
            verify(webClient, times(2)).post();

            // Verificamos que se intentó leer el cuerpo 3 veces
            verify(responseSpec, times(2)).bodyToMono(String.class);

            // Verificamos que el repositorio guardó los datos (persistió a pesar del error
            // de red)
            verify(cursoRepo, atLeastOnce()).save(any(Curso.class));
        }

        @Test
        void updateCurso_NewCurso_ErrorUpdateSSR()
                throws InternalServerException, InternalComunicationException, RepositoryException {
            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            when(cursoRepo.save(cursoGuardado)).thenReturn(cursoGuardado);
            doNothing().when(spyService).creaCarpetaCurso(cursoGuardado);
            doThrow(new InternalComunicationException("Error")).when(spyService).updateSSR();

            // Act
            InternalComunicationException ex = assertThrows(InternalComunicationException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_NewCurso_WithNullClases_SuccessfulUpdate()
                throws InternalServerException, InternalComunicationException, RepositoryException, NotFoundException {
            curso.setClasesCurso(null);

            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);

            // Act
            Curso resp = spyService.updateCurso(curso);

            // Assert
            assertNotNull(resp);
            assertEquals(cursoGuardado, resp);
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, null);
            verify(spyService).actualizarChatCurso(cursoGuardado);
            verify(spyService).actualizarStreamCurso(cursoGuardado);
            verify(spyService).updateSSR();
        }

        @Test
        void updateCurso_NewCurso_ErrorRepositoryInCrearClases()
                throws InternalServerException, RepositoryException {
            CursoService spyService = spy(cursoService);

            when(cursoRepo.save(curso)).thenReturn(cursoGuardado);
            when(claseRepo.save(any())).thenThrow(new IllegalArgumentException("save error"));

            // Act
            RepositoryException ex = assertThrows(RepositoryException.class,
                    () -> spyService.updateCurso(curso));

            // Assert
            assertTrue(ex.getMessage().contains("Error en guardar la clase"));
            verify(cursoRepo).save(curso);
            verify(spyService).creaCarpetaCurso(cursoGuardado);
            verify(spyService).creaClasesCurso(cursoGuardado, clases);
        }
    }

    // ==========================
    // Tests deleteCurso()
    // ==========================
    @Nested
    class DeleteCursoTests {

        private Long cursoId;
        private Curso curso;
        private Clase clase1;
        private Clase clase2;

        @BeforeEach
        void setUp() throws IOException {
            cursoId = 1L;
            clase1 = new Clase();
            clase1.setIdClase(10L);
            clase2 = new Clase();
            clase2.setIdClase(20L);
            curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setClasesCurso(List.of(clase1, clase2));
            clase1.setCursoClase(curso);
            clase2.setCursoClase(curso);

            File cursoFile = new File(tempDir.toString(), cursoId.toString());
            cursoFile.mkdir();
            File clase1File = new File(cursoFile, clase1.getIdClase().toString());
            clase1File.mkdir();
            File fileClase1 = new File(clase1File, "clase1.mp4");
            fileClase1.createNewFile();
            File clase2File = new File(cursoFile, clase2.getIdClase().toString());
            clase2File.mkdir();
            File fileClase2 = new File(clase2File, "clase2.mp4");
            fileClase2.createNewFile();

            clase1.setDireccionClase(fileClase1.getPath());
            clase2.setDireccionClase(fileClase2.getPath());
        }

        @Test
        void deleteCurso_SuccessfulDeletion() throws ServiceException {
            // Configuramos respuestas de éxito para las llamadas asíncronas
            // (1 para cursoStream, 1 para cursoChat, 1 para SSR, y 2 de cada para las
            // clases)
            lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            lenient().when(responseSpec.bodyToMono(any(Class.class)))
                    .thenReturn(Mono.just(true)) // Para Boolean.class (Stream)
                    .thenReturn(Mono.just("Clase chat borrado con exito!!!")); // Para String.class (Chat)

            CursoService spyService = spy(cursoService);
            doReturn(curso).when(spyService).getCurso(cursoId);

            when(claseRepo.findById(clase1.getIdClase())).thenReturn(Optional.of(clase1));
            when(claseRepo.findById(clase2.getIdClase())).thenReturn(Optional.of(clase2));

            // Act
            Boolean resp = spyService.deleteCurso(cursoId);

            // Assert
            assertTrue(resp);
            verify(spyService).getCurso(cursoId);
            verify(spyService, times(2)).deleteClase(any(Clase.class));
            verify(spyService).deleteCursoStream(cursoId);
            verify(spyService).deleteCursoChat(cursoId);

            // Verificamos que WebClient fue llamado realmente
            verify(webClient, atLeastOnce()).delete();
        }

        @Test
        void deleteCurso_SuccessfulDeletion_NoClases() throws ServiceException {
            curso.setClasesCurso(null);
            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Act
            Boolean resp = spyService.deleteCurso(cursoId);

            // Assert
            assertTrue(resp);

            verify(spyService).getCurso(cursoId);
            verify(spyService).deleteCarpetaCurso(cursoId);
            verify(spyService).deleteCursoStream(cursoId);
            verify(spyService).deleteCursoChat(cursoId);
            verify(spyService).updateSSR();

            // El repositorio debe borrar el curso
            verify(cursoRepo).deleteById(cursoId);
        }

        @Test
        void deleteCurso_ErrorCursoNoEncontrado() throws ServiceException {
            // Spy
            CursoService spyService = spy(cursoService);

            // Act
            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert
            assertTrue(ex.getMessage().contains("Error borrar las clases del curso con ID "));

            verify(spyService).getCurso(cursoId);
        }

        @Test
        void deleteCurso_ErrorDeleteClase() throws ServiceException {
            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase() debe lanzar excepción
            doThrow(new ServiceException("Error en borrar la clase")).when(spyService).deleteClase(any(Clase.class));

            // Act
            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert

            assertTrue(ex.getMessage().contains("Error borrar las clases del curso con ID "));
            verify(spyService).getCurso(cursoId);
        }

        @Test
        void deleteCurso_ErrorDeleteCarpeta() throws ServiceException {
            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase()
            doNothing().when(spyService).deleteClase(any(Clase.class));

            // Mock: deleteCarpetaCurso() lanza excepción
            doThrow(new InternalServerException("Error carpeta")).when(spyService).deleteCarpetaCurso(cursoId);

            // Act
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(spyService).getCurso(cursoId);
            verify(spyService, times(2)).deleteClase(any(Clase.class));
            verify(spyService).deleteCarpetaCurso(cursoId);
        }

        @Test
        void deleteCurso_ErrorDeleteStream() throws ServiceException {
            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase()
            doNothing().when(spyService).deleteClase(any(Clase.class));

            // Mock: deleteCursoStream() lanza excepción
            doThrow(new InternalComunicationException("Error stream")).when(spyService).deleteCursoStream(cursoId);

            // Act
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(spyService).getCurso(cursoId);
            verify(spyService, times(2)).deleteClase(any(Clase.class));
            verify(spyService).deleteCarpetaCurso(cursoId);
            verify(spyService).deleteCursoStream(cursoId);
        }

        @Test
        void deleteCurso_ErrorDeleteChat() throws ServiceException {
            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase()
            doNothing().when(spyService).deleteClase(any(Clase.class));

            // Mock: deleteCursoChat() lanza excepción
            doThrow(new InternalComunicationException("Error chat")).when(spyService).deleteCursoChat(cursoId);

            // Act
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(spyService).getCurso(cursoId);
            verify(spyService, times(2)).deleteClase(any(Clase.class));
            verify(spyService).deleteCarpetaCurso(cursoId);
            verify(spyService).deleteCursoStream(cursoId);
            verify(spyService).deleteCursoChat(cursoId);
        }

        @Test
        void deleteCurso_ErrorUpdateSSR() throws ServiceException {
            // Spy
            CursoService spyService = spy(cursoService);

            // Mock: getCurso() debe devolver el curso
            doReturn(curso).when(spyService).getCurso(cursoId);

            // Mock: deleteClase()
            doNothing().when(spyService).deleteClase(any(Clase.class));

            // Mock: updateSSR() lanza excepción
            doThrow(new InternalComunicationException("Error SSR")).when(spyService).updateSSR();

            // Act
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> spyService.deleteCurso(cursoId));

            // Assert
            assertTrue(ex.getMessage().contains("Error"));
            verify(spyService).getCurso(cursoId);
            verify(spyService, times(2)).deleteClase(any(Clase.class));
            verify(spyService).deleteCarpetaCurso(cursoId);
            verify(spyService).deleteCursoStream(cursoId);
            verify(spyService).deleteCursoChat(cursoId);
            verify(spyService).updateSSR();
        }

    }

    // ==========================
    // Tests getAll()
    // ==========================
    @Nested
    class GetAllTests {

        @Test
        void getAll_SuccessfulRetrieval() {
            List<Curso> cursos = List.of(new Curso(), new Curso());
            when(cursoRepo.findAll()).thenReturn(cursos);

            List<Curso> resp = cursoService.getAll();

            assertNotNull(resp);
            assertEquals(cursos, resp);
            verify(cursoRepo).findAll();
        }
    }

    // ==========================
    // Tests deleteClase()
    // ==========================
    @Nested
    class DeleteClaseTests {

        private Clase clase;

        @BeforeEach
        void setUp() {
            clase = new Clase();
            clase.setIdClase(1L);
            Curso c = new Curso();
            c.setIdCurso(100L);
            clase.setCursoClase(c);
        }

        @Test
        void deleteClase_SuccessfulDeletion_WithAsyncCoverage() throws ServiceException {
            when(claseRepo.findById(clase.getIdClase())).thenReturn(Optional.of(clase));

            // Configuramos WebClient para éxito
            lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            lenient().when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));
            lenient().when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.just("Clase chat borrado con exito!!!"));

            CursoService spyService = spy(cursoService);
            // NO hacemos doNothing en los métodos asíncronos para que se ejecuten
            doNothing().when(spyService).deleteCarpetaClase(clase);
            doNothing().when(spyService).updateSSR();

            spyService.deleteClase(clase);

            verify(claseRepo).delete(clase);
            verify(spyService).deleteClaseStream(clase);
            verify(spyService).deleteClaseChat(clase);
        }

        @Test
        void deleteClase_ErrorAsyncFlatMapCoverage() throws ServiceException {
            when(claseRepo.findById(clase.getIdClase())).thenReturn(Optional.of(clase));

            // 1. Mock de respuesta de error HTTP
            org.springframework.web.reactive.function.client.ClientResponse mockResponse = mock(
                    org.springframework.web.reactive.function.client.ClientResponse.class);
            when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("Error de Servidor"));

            // 2. Interceptamos onStatus para ejecutar el flatMap (Pone el flatMap en VERDE)
            lenient().when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
                java.util.function.Function<org.springframework.web.reactive.function.client.ClientResponse, Mono<? extends Throwable>> flatMapFunc = invocation
                        .getArgument(1);
                flatMapFunc.apply(mockResponse).subscribe();
                return responseSpec;
            });

            // 3. Forzamos el error para que pase por onErrorResume
            lenient().when(responseSpec.bodyToMono(any(Class.class)))
                    .thenReturn(Mono.error(new RuntimeException("Conexion Fallida")));

            CursoService spyService = spy(cursoService);
            doNothing().when(spyService).deleteCarpetaClase(clase);
            doNothing().when(spyService).updateSSR();

            // Act
            spyService.deleteClase(clase);

            // Assert: El método no lanza excepción hacia fuera porque tienes .onErrorResume
            // con Mono.empty()
            verify(claseRepo).delete(clase);
            verify(responseSpec, atLeastOnce()).onStatus(any(), any());
        }
    }

    // ==========================
    // Tests subeVideo()
    // ==========================
    @Nested
    class SubeVideoTests {

        @Test
        void subeVideo_SuccessfulUpload() throws InternalServerException, IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("video.mp4");
            when(file.getBytes()).thenReturn("test content".getBytes());

            String resp = cursoService.subeVideo(file);

            assertNotNull(resp);
            assertTrue(resp.contains("video.mp4"));
        }

        @Test
        void subeVideo_SuccessfulUpload_NoFileName() throws InternalServerException, IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn(null);
            when(file.getBytes()).thenReturn("test content".getBytes());

            String resp = cursoService.subeVideo(file);

            assertNotNull(resp);
            assertTrue(resp.contains("unknown_file"));
        }

        @Test
        void subeVideo_ErrorIOException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("video.mp4");
            when(file.getBytes()).thenThrow(new IOException("io error"));

            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> cursoService.subeVideo(file));

            assertTrue(ex.getMessage().contains("Error de IO al subir el video"));
        }

        @Test
        void subeVideo_ErrorIllegalArgumentException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("video.mp4");
            when(file.getBytes()).thenThrow(new IllegalArgumentException("arg error"));

            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> cursoService.subeVideo(file));

            assertTrue(ex.getMessage().contains("Error al subir el video"));
        }

        @Test
        void subeVideo_ErrorIllegalStateException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("video.mp4");
            when(file.getBytes()).thenThrow(new IllegalStateException("state error"));

            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> cursoService.subeVideo(file));

            assertTrue(ex.getMessage().contains("Error al subir el video"));
        }

        @Test
        void subeVideo_ErrorRuntimeException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("video.mp4");
            when(file.getBytes()).thenThrow(new RuntimeException("runtime error"));

            InternalServerException ex = assertThrows(InternalServerException.class,
                    () -> cursoService.subeVideo(file));

            assertTrue(ex.getMessage().contains("Error inesperado al subir el video"));
        }
    }

    // --- Mocks de Repositorios y Servicios ---
    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private PlanRepository planRepo;
    @Mock
    private InitAppService initAppService;
    @Mock
    private WebClientConfig webClientConfig;

    // --- Mocks de WebClient ---
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CursoService cursoService;
    private final String backStreamURL = "http://mocked-stream-url";
    private final String backChatURL = "http://mocked-chat-url";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws SSLException, URISyntaxException {
        // 1. Entrada
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);

        // 2. DELETE Chain
        lenient().doReturn(requestHeadersUriSpec).when(webClient).delete();
        lenient().doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());

        // 3. POST Chain
        lenient().doReturn(requestBodyUriSpec).when(webClient).post();
        lenient().doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        lenient().doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        lenient().doReturn(requestHeadersSpec).when(requestBodySpec).body(any(), any(Class.class));

        // 4. Response Chain
        lenient().doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        lenient().doReturn(responseSpec).when(responseSpec).onStatus(any(), any());

        // IMPORTANTE: Devolver un Mono vacío por defecto para que no falle el
        // .onErrorResume()
        lenient().doReturn(Mono.empty()).when(responseSpec).bodyToMono(any(Class.class));

        // 5. Instanciación
        cursoService = new CursoService(
                tempDir.toString(),
                backStreamURL,
                backChatURL,
                cursoRepo,
                claseRepo,
                usuarioRepo,
                initAppService,
                webClientConfig);
    }
}