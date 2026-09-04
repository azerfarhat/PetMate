package com.petmate.backend;

import com.petmate.backend.config.AppProperties;
import com.petmate.backend.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, StorageProperties.class})
public class PetMateBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetMateBackendApplication.class, args);
	}

}
