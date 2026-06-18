# Barber Booking API

API backend para reservas de barbería construida con Java 21, Spring Boot 3,
PostgreSQL, Flyway, Testcontainers y arquitectura hexagonal.

## Estado actual

Foundation inicial del proyecto:

- Proyecto Spring Boot con Maven Wrapper.
 - Configuración por variables de entorno.
- Docker Compose local para PostgreSQL.
 - Migración inicial de base de datos.
 - Modelo de dominio base y puertos de aplicación.
 - Documentación OpenAPI/Swagger y respuestas de error estandarizadas.
 - Reglas de negocio centralizadas: `docs/business-rules.md`.
 - Swagger UI disponible en tiempo de ejecución: `http://localhost:8080/swagger-ui/index.html`.
 - Swagger UI disponible en tiempo de ejecucion: `http://localhost:8080/swagger-ui/index.html`.
- CRUD de clientes con soft delete logico.
- CRUD de barberos con activacion y desactivacion logica.
- CRUD de servicios ofrecidos con activacion y desactivacion logica.
- Gestion de horarios laborales por barbero con control de cruces.
- Reserva de citas online con validacion de disponibilidad y horario laboral.
- Consulta de disponibilidad por slots segun servicio, horario laboral y citas activas.
- Agenda diaria operativa del barbero con resumen por estado y ocupacion.
- Ciclo de vida de citas con transiciones controladas de estado.
- Citas presenciales walk-in con inicio inmediato opcional.
- Foundation Auth/RBAC con bootstrap, login JWT, roles y tenant desde token.

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
   `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_MINUTES` y `APP_BOOTSTRAP_TOKEN`
   configuran autenticacion local. En produccion deben venir de variables de
   entorno seguras.

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

Bootstrap del primer usuario:

```text
POST http://localhost:8080/api/v1/auth/bootstrap
X-Bootstrap-Token: <APP_BOOTSTRAP_TOKEN>
```

Despues usa `POST /api/v1/auth/login` y envia el JWT como
`Authorization: Bearer <token>` para endpoints administrativos.

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
- HU-09: Ciclo de vida de la cita.
- HU-10: Citas presenciales walk-in.
- HU-17: Multi-tenant foundation.
- HU-18: Auth and RBAC foundation.

## Documentación

En este repositorio encontrarás la documentación principal del proyecto:

- `docs/architecture.md` — Arquitectura del proyecto.
- `docs/api-contract.md` — Contrato de la API y ejemplos.
- `docs/business-rules.md` — Reglas de negocio centrales.
- `docs/quality-audit.md` — Auditoría de calidad y recomendaciones.
- `docs/technical-debt.md` — Deuda técnica y notas de decisión.
- `docs/pre-push-checklist.md` — Lista de comprobación antes de subir el repositorio.


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
