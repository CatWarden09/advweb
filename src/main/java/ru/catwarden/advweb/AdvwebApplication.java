package ru.catwarden.advweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdvwebApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdvwebApplication.class, args);
	}

}
