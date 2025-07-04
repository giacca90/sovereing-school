package com.sovereingschool.back_streaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EntityScan(basePackages = "com.sovereingschool.back_common.Models")
@EnableJpaRepositories(basePackages = {
		"com.sovereingschool.back_streaming.Repositories", // si aún usás repos locales
		"com.sovereingschool.back_common.Repositories" // para los repos que moviste
})
@ComponentScan(basePackages = {
		"com.sovereingschool.back_streaming",
		"com.sovereingschool.back_common"
})
@SpringBootApplication
@EnableAsync
public class BackStreamingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackStreamingApplication.class, args);
	}

}
