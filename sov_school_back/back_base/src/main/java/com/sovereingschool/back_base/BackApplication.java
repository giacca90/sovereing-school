package com.sovereingschool.back_base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EntityScan(basePackages = "com.sovereingschool.back_common.Models")
@EnableJpaRepositories(basePackages = {
		"com.sovereingschool.back_base.Repositories",
		"com.sovereingschool.back_common.Repositories" })
@ComponentScan(basePackages = {
		"com.sovereingschool.back_base",
		"com.sovereingschool.back_common"
})
@SpringBootApplication
@EnableAsync
public class BackApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackApplication.class, args);
	}

}
