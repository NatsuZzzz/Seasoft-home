package com.store.seasoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeasoftApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeasoftApplication.class, args);
	}

}
