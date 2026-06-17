# AGENTS

## Reglas del proyecto

- Responder en espanol.
- Entender el contexto antes de modificar codigo.
- No trabajar directamente sobre `main`, `master`, `develop`, `qa` ni ramas
  protegidas.
- No imprimir ni commitear secretos.
- Usar variables de entorno para configuracion sensible.
- Mantener `.env.example` sin valores reales.
- Mantener arquitectura hexagonal: `domain`, `application`, `infrastructure`.
- No poner logica de negocio en controladores.
- Validar build, tests y estado Git antes de reportar una entrega como lista.

## Stack base

- Java 21.
- Spring Boot 3.
- PostgreSQL.
- Flyway.
- JUnit 5.
- Testcontainers.
- Docker Compose.
