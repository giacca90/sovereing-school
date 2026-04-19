package com.sovereingschool.back_streaming.Models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Clase de pruebas unitarias para el modelo StatusClase.
 * Verifica la lógica de cálculo de progreso y el comportamiento de los métodos
 * generados por Lombok.
 */
class StatusClaseTest {

    /**
     * Prueba que el porcentaje de progreso sea 0 cuando no hay segmentos totales.
     */
    @Test
    void testGetProgressPercentage_ZeroSegments() {
        StatusClase status = new StatusClase();
        status.setTotalSegments(0);
        assertEquals(0.0, status.getProgressPercentage(),
                "El porcentaje de progreso debe ser 0.0 cuando no hay segmentos totales.");
    }

    /**
     * Prueba que el porcentaje de progreso sea 0 cuando los segmentos totales son
     * negativos.
     */
    @Test
    void testGetProgressPercentage_NegativeSegments() {
        StatusClase status = new StatusClase();
        status.setTotalSegments(-1);
        assertEquals(0.0, status.getProgressPercentage(),
                "El porcentaje de progreso debe ser 0.0 cuando los segmentos totales son negativos.");
    }

    /**
     * Prueba el cálculo normal del porcentaje de progreso con algunos segmentos
     * completados.
     */
    @Test
    void testGetProgressPercentage_NormalProgress() {
        StatusClase status = new StatusClase();
        status.setTotalSegments(10);
        Set<Integer> progress = new HashSet<>();
        progress.add(1);
        progress.add(2);
        status.setProgress(progress);

        assertEquals(20.0, status.getProgressPercentage(),
                "El porcentaje de progreso debe ser 20.0 para 2 de 10 segmentos.");
    }

    /**
     * Prueba que el porcentaje de progreso sea 100 cuando todos los segmentos están
     * completados.
     */
    @Test
    void testGetProgressPercentage_FullProgress() {
        StatusClase status = new StatusClase();
        status.setTotalSegments(2);
        Set<Integer> progress = new HashSet<>();
        progress.add(0);
        progress.add(1);
        status.setProgress(progress);

        assertEquals(100.0, status.getProgressPercentage(),
                "El porcentaje de progreso debe ser 100.0 cuando todos los segmentos están completados.");
    }

    /**
     * Prueba que el porcentaje de progreso se limite a 100 cuando hay más segmentos
     * completados que totales.
     */
    @Test
    void testGetProgressPercentage_OverProgress() {
        StatusClase status = new StatusClase();
        status.setTotalSegments(1);
        Set<Integer> progress = new HashSet<>();
        progress.add(0);
        progress.add(1); // More than total
        status.setProgress(progress);

        assertEquals(100.0, status.getProgressPercentage(), "El porcentaje de progreso debe limitarse a 100.0.");
    }

    /**
     * Prueba los métodos generados por Lombok (getters, setters, equals, hashCode,
     * toString).
     */
    @Test
    void testLombokMethods() {
        StatusClase status1 = StatusClase.builder()
                .idClase(1L)
                .completed(true)
                .totalSegments(100)
                .build();

        assertEquals(1L, status1.getIdClase(), "El ID de la clase debe coincidir.");
        assertTrue(status1.isCompleted(), "El estado debe ser completado.");
        assertEquals(100, status1.getTotalSegments(), "El total de segmentos debe ser 100.");
        assertNotNull(status1.getProgress(), "El conjunto de progreso no debe ser nulo.");

        StatusClase status2 = new StatusClase(2L, false, 50, new HashSet<>());
        assertEquals(2L, status2.getIdClase(), "El ID de la clase debe coincidir.");
        assertFalse(status2.isCompleted(), "El estado no debe ser completado.");

        status1.setIdClase(3L);
        assertEquals(3L, status1.getIdClase(), "El ID de la clase actualizado debe coincidir.");

        assertNotEquals(status1, status2, "Los estados deben ser diferentes.");
        assertNotNull(status1.toString(), "La representación en cadena no debe ser nula.");
        assertEquals(status1.hashCode(), status1.hashCode(), "El hashCode debe ser consistente.");
    }
}
