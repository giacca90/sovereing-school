package com.sovereingschool.back_base.Configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@Configuration
public class JacksonConfig {

    @Bean
    public Module hibernateModule() {
        Hibernate6Module module = new Hibernate6Module();

        // ⚙️ Opciones recomendadas:
        // No forzar la carga de relaciones LAZY (evita que se dispare sin querer un
        // montón de queries)
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);

        return module;
    }
}
