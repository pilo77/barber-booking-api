package com.villamil.barberbooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "app.jwt.secret=test-jwt-secret-for-integration-test-at-least-32-chars")
class BarberBookingApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
