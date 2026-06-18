# Barber Booking API

API backend para reservas de barberia construida con Java 21, Spring Boot 3,
PostgreSQL, Flyway, Testcontainers y arquitectura hexagonal.

## Estado actual

Foundation inicial del proyecto:

- Proyecto Spring Boot con Maven Wrapper.
- Configuracion por variables de entorno.
- Docker Compose local para PostgreSQL.
- Migracion inicial de base de datos.
- Modelo de dominio base y puertos de aplicacion.
- Documentacion de arquitectura, contrato API y flujo Git.
- CRUD de clientes con soft delete logico.
- CRUD de barberos con activacion y desactivacion logica.
- CRUD de servicios ofrecidos con activacion y desactivacion logica.
- Gestion de horarios laborales por barbero con control de cruces.
- Reserva de citas online con validacion de disponibilidad y horario laboral.
- Consulta de disponibilidad por slots segun servicio, horario laboral y citas activas.
- Agenda diaria operativa del barbero con resumen por estado y ocupacion.

## Requisitos

- Java 21.
- Docker Desktop.
- PowerShell.

No se requiere Maven global. Usa `mvnw.cmd`.

## Configuracion local

1. Copia el archivo de ejemplo:

```powershell
Copy-Item .env.example .env
```

2. Edita `.env` y reemplaza los placeholders. No subas `.env` al repositorio.

   `BOOKING_SLOT_STEP_MINUTES` controla cada cuantos minutos se evalua un
   inicio posible de cita. El valor por defecto es `15`.

3. Levanta PostgreSQL:

```powershell
docker compose up -d postgres
```

4. Ejecuta validacion completa:

```powershell
.\mvnw.cmd clean verify
```

Si Docker Desktop no esta corriendo, los tests de integracion con
Testcontainers se omiten y quedan activos los tests unitarios. Para validar el
contexto con PostgreSQL real, abre Docker Desktop y vuelve a ejecutar el comando.

5. Ejecuta la API:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```text
GET http://localhost:8080/actuator/health
```

## Arquitectura

El proyecto sigue arquitectura hexagonal:

- `domain`: reglas y modelos de negocio sin dependencias de Spring, JPA o HTTP.
- `application`: casos de uso, DTOs y puertos de entrada/salida.
- `infrastructure`: adaptadores REST, persistencia, configuracion e integraciones.

Los controladores no deben contener reglas de negocio. Los casos de uso dependen
de puertos, no de repositorios JPA ni de detalles de PostgreSQL.

## Roadmap por historias

- HU-01: Foundation del proyecto y estructura hexagonal.
- HU-02: CRUD de clientes.
- HU-03: CRUD de barberos.
- HU-04: CRUD de servicios ofrecidos.
- HU-05: Horarios laborales por barbero.
- HU-06: Agendar cita online.
- HU-07: Consultar disponibilidad.
- HU-08: Agenda diaria operativa del barbero.
- HU-09: Walk-ins.
- HU-10: Cambios de estado de cita.

## Git

No trabajar sobre `main`, `master`, `develop` o ramas protegidas.

Rama actual recomendada para esta foundation:

```text
feature/HU-01-project-foundation
```

Commits con Conventional Commits, por ejemplo:

```text
chore(project): bootstrap barber booking api foundation
```

Ver mas detalles en `docs/git-workflow.md`.
