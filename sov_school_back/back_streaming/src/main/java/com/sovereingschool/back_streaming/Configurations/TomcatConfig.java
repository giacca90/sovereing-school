package com.sovereingschool.back_streaming.Configurations;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return factory -> {
            // Conector HTTP
            Connector httpConnector = new Connector(
                    org.springframework.boot.tomcat.TomcatWebServerFactory.DEFAULT_PROTOCOL);
            httpConnector.setScheme("http");
            httpConnector.setPort(8091); // Puerto HTTP interno
            httpConnector.setSecure(false);

            factory.addAdditionalConnectors(httpConnector);
        };
    }
}
