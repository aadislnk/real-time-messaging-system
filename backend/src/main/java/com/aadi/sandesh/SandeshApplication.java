package com.aadi.sandesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SandeshApplication {

	public static void main(String[] args) {
		SpringApplication.run(SandeshApplication.class, args);
	}

}
