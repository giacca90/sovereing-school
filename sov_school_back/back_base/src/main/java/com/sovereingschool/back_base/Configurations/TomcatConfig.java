package com.sovereingschool.back_base.Configurations;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    // Lee de application.properties, si no existe usa 8081 por defecto
    @Value("${server.http.port:8081}")
    private int httpPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return factory -> {
            // Si el puerto es 0, Spring Tomcat entenderá que debe ser aleatorio
            if (httpPort >= 0) {
                Connector httpConnector = new Connector(
                        org.springframework.boot.tomcat.TomcatWebServerFactory.DEFAULT_PROTOCOL);
                httpConnector.setScheme("http");
                httpConnector.setPort(httpPort);
                httpConnector.setSecure(false);
                factory.addAdditionalConnectors(httpConnector);
            }
        };
    }
}
