package com.graphqlguy.moviedb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.security.Security;

//without this, @PreAuthorize annotations are silently ignored. This is one of the most common Spring Security mistakes.
@EnableMethodSecurity
@SpringBootApplication
@ConfigurationPropertiesScan
public class MovieDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieDbApplication.class, args);
	}

}