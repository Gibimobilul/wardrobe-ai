package com.gilbert.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "com.gilbert.demo")
public class WardrobeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WardrobeApplication.class, args);
	}

}
