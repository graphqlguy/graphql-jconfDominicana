package com.graphqlguy.moviedb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MovieDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieDbApplication.class, args);
	}

}