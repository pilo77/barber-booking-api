package com.villamil.barberbooking.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
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
