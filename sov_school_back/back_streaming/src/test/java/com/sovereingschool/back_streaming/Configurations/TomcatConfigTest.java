package com.sovereingschool.back_streaming.Configurations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * Tests para validar la configuración de Tomcat.
 * 
 * Verifica que el customizador de Tomcat se cree y configure correctamente
 * para añadir el conector HTTP adicional en el puerto 8091.
 */
@SpringBootTest
@DisplayName("TomcatConfig - Configuración de Servidor Tomcat")
class TomcatConfigTest {

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("servletContainer")
    private WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer;

    @Test
    @DisplayName("debe crear el WebServerFactoryCustomizer correctamente")
    void shouldCreateServletContainerCustomizer() {
        assertNotNull(servletContainerCustomizer,
                "El servletContainerCustomizer no debe ser null");
    }

    @Test
    @DisplayName("debe customizar la factory de Tomcat")
    void shouldCustomizeTomcatFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        assertDoesNotThrow(() -> servletContainerCustomizer.customize(factory),
                "La customización de Tomcat debe ejecutarse sin excepciones");
    }

    @Test
    @DisplayName("debe añadir conectores adicionales a la factory")
    void shouldAddAdditionalConnectors() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        int connectorsBeforeCustomization = factory.getAdditionalConnectors().size();

        servletContainerCustomizer.customize(factory);
        int connectorsAfterCustomization = factory.getAdditionalConnectors().size();

        assertTrue(connectorsAfterCustomization > connectorsBeforeCustomization,
                "Debe añadirse al menos un conector adicional después de la customización");
    }
}
