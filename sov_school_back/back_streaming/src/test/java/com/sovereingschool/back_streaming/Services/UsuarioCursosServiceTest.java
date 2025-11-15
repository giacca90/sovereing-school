package com.sovereingschool.back_streaming.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_streaming.Repositories.UsuarioCursosRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioCursosServiceTest {

    // ==========================
    // Tests syncUserCourses()
    // ==========================
    @Nested
    class SyncUserCoursesTests {
    }

    // ==========================
    // Tests addNuevoUsuario()
    // ==========================
    @Nested
    class AddNuevoUsuarioTests {
    }

    // ==========================
    // Tests getClase()
    // ==========================
    @Nested
    class GetClaseTests {
    }

    // ==========================
    // Tests addClase()
    // ==========================
    @Nested
    class AddClaseTests {
    }

    // ==========================
    // Tests deleteClase()
    // ==========================
    @Nested
    class DeleteClaseTests {
    }

    // ==========================
    // Tests getStatus()
    // ==========================
    @Nested
    class GetStatusTests {
    }

    // ==========================
    // Tests actualizarCursoStream()
    // ==========================
    @Nested
    class ActualizarCursoStreamTests {
    }

    // ==========================
    // Tests deleteUsuarioCursos()
    // ==========================
    @Nested
    class DeleteUsuarioCursosTests {
    }

    // ==========================
    // Tests deleteCurso()
    // ==========================
    @Nested
    class DeleteCursoTests {
    }

    @Mock
    private StreamingService streamingService;
    @Mock
    private UsuarioRepository usuarioRepository; // Repositorio de PostgreSQL para usuarios
    @Mock
    private CursoRepository cursoRepository; // Repositorio de PostgreSQL para clases
    @Mock
    private ClaseRepository claseRepository; // Repositorio de PostgreSQL para clases
    @Mock
    private UsuarioCursosRepository usuarioCursosRepository; // Repositorio de MongoDB
    @Mock
    private MongoTemplate mongoTemplate;

    private UsuarioCursosService usuarioCursosService;

    @BeforeEach
    void setUp() {
        usuarioCursosService = new UsuarioCursosService(streamingService, usuarioRepository, cursoRepository,
                claseRepository, usuarioCursosRepository, mongoTemplate);
    }

}
