package com.villamil.barberbooking;

import org.springframework.boot.SpringApplication;

public class TestBarberBookingApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(BarberBookingApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
