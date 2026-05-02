package com.sovereingschool.back_streaming.Repositories;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.sovereingschool.back_streaming.Models.UsuarioCursos;

/**
 * Pruebas unitarias para {@link UsuarioCursosRepositoryImpl}.
 */
class UsuarioCursosRepositoryTest {

    private MongoTemplate mongoTemplate;
    private UsuarioCursosRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        repository = new UsuarioCursosRepositoryImpl(mongoTemplate);
    }

    /**
     * Verifica que la actualización del progreso se realice correctamente.
     */
    @Test
    void testUpdateProgress_ShouldUpdateCorrectly() {
        // Given
        final Long idUsuario = 1L;
        final Long idCurso = 2L;
        final Long idClase = 3L;
        final int segmentIndex = 5;

        // When
        repository.updateProgress(idUsuario, idCurso, idClase, segmentIndex);

        // Then
        final ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        final ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(UsuarioCursos.class));

        final Query capturedQuery = queryCaptor.getValue();
        final Update capturedUpdate = updateCaptor.getValue();

        assertNotNull(capturedQuery, "La consulta no debería ser nula");
        assertNotNull(capturedUpdate, "La actualización no debería ser nula");
    }

    private void assertNotNull(final Object obj, final String message) {
        if (obj == null)
            throw new AssertionError(message);
    }
}
