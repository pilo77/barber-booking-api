package com.villamil.barberbooking.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT"
)
@OpenAPIDefinition(
        info = @Info(
                title = "Barber Booking API",
                version = "v1",
                description = "API para reservas y gestión de barberías",
                contact = @Contact(name = "Barber Booking Team")
        )
)
public class OpenApiConfig {

}
