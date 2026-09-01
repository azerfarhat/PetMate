package com.pawmate.backend;

import com.pawmate.backend.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PawmateBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PawmateBackendApplication.class, args);
	}

}
