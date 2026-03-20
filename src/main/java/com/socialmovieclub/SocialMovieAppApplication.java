package com.socialmovieclub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EntityScan("com.socialmovieclub.entity")
@EnableJpaRepositories("com.socialmovieclub.repository")
@EnableAsync
@EnableJpaAuditing
public class SocialMovieAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocialMovieAppApplication.class, args);
	}

}
