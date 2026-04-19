package com.sovereingschool.back_streaming.Repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Pruebas unitarias para {@link UsuarioCursosRepository}.
 */
class UsuarioCursosRepositoryTest {

    /**
     * Verifica que la actualización del progreso se realice correctamente.
     */
    @Test
    void testUpdateProgress_ShouldUpdateCorrectly() {
        // Given
        final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        final UsuarioCursosRepository repository = mock(UsuarioCursosRepository.class);

        // Hacemos que llame al método default real
        doCallRealMethod().when(repository).updateProgress(anyLong(), anyLong(), anyLong(), anyInt(),
                any(MongoTemplate.class));

        final Long idUsuario = 1L;
        final Long idCurso = 2L;
        final Long idClase = 3L;
        final int segmentIndex = 5;

        // When
        repository.updateProgress(idUsuario, idCurso, idClase, segmentIndex, mongoTemplate);

        // Then
        final ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        final ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq("user_courses"));

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
