package ru.catwarden.advweb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class AdvwebApplicationTests {

	@Test
	void applicationClassIsDiscoverable() {
		SpringApplication application = new SpringApplication(AdvwebApplication.class);
		application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
	}

}
