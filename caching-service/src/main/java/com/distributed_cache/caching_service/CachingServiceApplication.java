package com.distributed_cache.caching_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CachingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CachingServiceApplication.class, args);
	}

    @Bean
    ModelMapper modelMapper(){
        return new ModelMapper();
    }

    @Bean
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

}
