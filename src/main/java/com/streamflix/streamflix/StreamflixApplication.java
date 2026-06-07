package com.streamflix.streamflix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.streamflix")
@EnableJpaRepositories(basePackages = "com.streamflix.repositorio")
@EntityScan(basePackages = "com.streamflix.modelo")
public class StreamflixApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamflixApplication.class, args);
	}

}
