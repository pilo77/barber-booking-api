# Architecture

## Objetivo

Construir una API de reservas para barberia con bajo acoplamiento, reglas de
negocio testeables y separacion clara entre dominio, aplicacion e
infraestructura.

## Capas

### Domain

Paquete: `com.villamil.barberbooking.domain`

Contiene modelos, value objects y excepciones de negocio. No debe importar
Spring, JPA, HTTP, JSON ni clases de infraestructura.

### Application

Paquete: `com.villamil.barberbooking.application`

Contiene casos de uso, comandos, respuestas y puertos.

- `port.in`: capacidades que ofrece el sistema.
- `port.out`: dependencias que la aplicacion necesita del exterior.
- `service`: implementaciones de casos de uso.
- `dto`: comandos y respuestas.

### Infrastructure

Paquete: `com.villamil.barberbooking.infrastructure`

Contiene adaptadores web, persistencia, configuracion e integraciones externas.
Aqui si pueden vivir Spring MVC, Spring Data JPA, PostgreSQL, Flyway y OpenAPI.

## Reglas de arquitectura

- Los controladores solo traducen HTTP a comandos y respuestas.
- La logica de negocio vive en dominio o servicios de aplicacion.
- Los servicios de aplicacion dependen de puertos, no de JPA.
- Las entidades JPA no se exponen directamente como respuestas HTTP.
- Las migraciones de esquema se versionan con Flyway.
- La base de datos puede reforzar consistencia, pero no reemplaza las reglas de
  negocio del caso de uso.

## Modelo inicial

- `Customer`: cliente de la barberia.
- `Barber`: barbero que atiende citas.
- `ServiceOffering`: servicio ofrecido, con duracion, precio y activacion logica.
- `BarberWorkingHour`: horario laboral recurrente por dia y barbero.
- `Appointment`: reserva o atencion walk-in.

## Regla anti doble reserva

La aplicacion debe validar cruces antes de guardar una cita. La base de datos
tambien refuerza la regla con una restriccion de exclusion sobre citas activas.

Una cita se cruza con otra cuando:

```text
existing.startAt < new.endAt AND existing.endAt > new.startAt
```
