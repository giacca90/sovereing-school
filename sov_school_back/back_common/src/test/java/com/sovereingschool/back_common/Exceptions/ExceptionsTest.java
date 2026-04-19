package com.sovereingschool.back_common.Exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para validar las excepciones personalizadas del sistema.
 */
class ExceptionsTest {

    /**
     * Prueba la excepción {@link InternalComunicationException}.
     */
    @Test
    void testInternalComunicationException_DeberiaAlmacenarMensajeYCAusa() {
        InternalComunicationException ex = new InternalComunicationException("message");
        assertEquals("message", ex.getMessage(), "El mensaje debería coincidir");

        Exception cause = new RuntimeException("cause");
        InternalComunicationException exWithCause = new InternalComunicationException("message", cause);
        assertEquals("message", exWithCause.getMessage(), "El mensaje debería coincidir");
        assertEquals(cause, exWithCause.getCause(), "La causa debería coincidir");
    }

    /**
     * Prueba la excepción {@link InternalServerException}.
     */
    @Test
    void testInternalServerException_DeberiaAlmacenarMensajeYCAusa() {
        InternalServerException ex = new InternalServerException("message");
        assertEquals("message", ex.getMessage(), "El mensaje debería coincidir");

        Exception cause = new RuntimeException("cause");
        InternalServerException exWithCause = new InternalServerException("message", cause);
        assertEquals("message", exWithCause.getMessage(), "El mensaje debería coincidir");
        assertEquals(cause, exWithCause.getCause(), "La causa debería coincidir");
    }

    /**
     * Prueba la excepción {@link NotFoundException}.
     */
    @Test
    void testNotFoundException_DeberiaAlmacenarMensajeYCAusa() {
        NotFoundException ex = new NotFoundException("message");
        assertEquals("message", ex.getMessage(), "El mensaje debería coincidir");

        Exception cause = new RuntimeException("cause");
        NotFoundException exWithCause = new NotFoundException("message", cause);
        assertEquals("message", exWithCause.getMessage(), "El mensaje debería coincidir");
        assertEquals(cause, exWithCause.getCause(), "La causa debería coincidir");
    }

    /**
     * Prueba la excepción {@link RepositoryException}.
     */
    @Test
    void testRepositoryException_DeberiaAlmacenarMensajeYCAusa() {
        RepositoryException ex = new RepositoryException("message");
        assertEquals("message", ex.getMessage(), "El mensaje debería coincidir");

        Exception cause = new RuntimeException("cause");
        RepositoryException exWithCause = new RepositoryException("message", cause);
        assertEquals("message", exWithCause.getMessage(), "El mensaje debería coincidir");
        assertEquals(cause, exWithCause.getCause(), "La causa debería coincidir");
    }

    /**
     * Prueba la excepción {@link ServiceException}.
     */
    @Test
    void testServiceException_DeberiaAlmacenarMensajeYCAusa() {
        ServiceException ex = new ServiceException("message");
        assertEquals("message", ex.getMessage(), "El mensaje debería coincidir");

        Exception cause = new RuntimeException("cause");
        ServiceException exWithCause = new ServiceException("message", cause);
        assertEquals("message", exWithCause.getMessage(), "El mensaje debería coincidir");
        assertEquals(cause, exWithCause.getCause(), "La causa debería coincidir");
    }
}
