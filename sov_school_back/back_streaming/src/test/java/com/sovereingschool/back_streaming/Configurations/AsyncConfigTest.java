package com.sovereingschool.back_streaming.Configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Tests para validar la configuración de ejecución asincrónica.
 * 
 * Verifica que el bean taskExecutor se cree correctamente con los parámetros
 * especificados para el pool de hilos.
 */
@SpringBootTest
@DisplayName("AsyncConfig - Configuración de Ejecución Asincrónica")
class AsyncConfigTest {

    @Autowired
    private Executor taskExecutor;

    @Test
    @DisplayName("debe crear el bean taskExecutor correctamente")
    void shouldCreateTaskExecutorBean() {
        assertNotNull(taskExecutor, "El bean taskExecutor no debe ser null");
    }

    @Test
    @DisplayName("debe ser una instancia de ThreadPoolTaskExecutor")
    void shouldBeThreadPoolTaskExecutor() {
        assertInstanceOf(ThreadPoolTaskExecutor.class, taskExecutor,
                "El executor debe ser instancia de ThreadPoolTaskExecutor");
    }

    @Test
    @DisplayName("debe configurar corePoolSize en 10")
    void shouldHaveCorePoolSize10() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertEquals(10, executor.getCorePoolSize(),
                "El corePoolSize debe ser 10");
    }

    @Test
    @DisplayName("debe configurar maxPoolSize en 30")
    void shouldHaveMaxPoolSize30() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertEquals(30, executor.getMaxPoolSize(),
                "El maxPoolSize debe ser 30");
    }

    @Test
    @DisplayName("debe configurar queueCapacity en 100")
    void shouldHaveQueueCapacity100() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertEquals(100, executor.getQueueCapacity(),
                "El queueCapacity debe ser 100");
    }

    @Test
    @DisplayName("debe configurar threadNamePrefix como 'AsyncTask-'")
    void shouldHaveCorrectThreadNamePrefix() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertEquals("AsyncTask-", executor.getThreadNamePrefix(),
                "El prefijo de nombre de hilo debe ser 'AsyncTask-'");
    }

    @Test
    @DisplayName("debe estar inicializado")
    void shouldBeInitialized() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertTrue(executor.getThreadPoolExecutor() != null,
                "El executor debe estar inicializado");
    }
}
