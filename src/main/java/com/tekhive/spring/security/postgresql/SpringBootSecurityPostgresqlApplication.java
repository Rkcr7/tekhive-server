package com.tekhive.spring.security.postgresql;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Configuration
@EnableWebMvc
@SpringBootApplication
public class SpringBootSecurityPostgresqlApplication {



	public static void main(String[] args) {
		SpringApplication.run(SpringBootSecurityPostgresqlApplication.class, args);
	}

}
