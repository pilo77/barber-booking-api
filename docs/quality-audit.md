# Quality Audit — Backend

Fecha: 2026-06-17

## Resumen general

- Estado: listo para integrar en `develop` (HU-11 integrado localmente).
- Build y tests: `mvnw verify` -> BUILD SUCCESS. 141 tests, 0 failures, 0 errors, 1 skipped (Testcontainers environment no encontrado).
- Migraciones Flyway: ordenadas V1..V7, forward-only.
- Documentación: `docs/architecture.md`, `docs/api-contract.md`, `docs/business-rules.md` añadidas/actualizadas.

## Hallazgos críticos

- Ninguno que impida merge o despliegue inmediato. Los tests pasan y no hay errores de compilación.

## Hallazgos importantes

- `AppointmentBookingPolicy` vive en `application.service` y centraliza validaciones (correcto), pero está anotada con Spring `@Component`. Esto introduce una dependencia de framework en la capa de aplicación. Recomendación: convertirla en una clase pura y exponerla como bean desde la capa `infrastructure` (configuración), o documentar la decisión arquitectónica.
- `GlobalExceptionHandler` estandariza errores correctamente y mapea códigos HTTP acorde a las reglas (400, 404, 409). Buena práctica aplicada.
- Controladores (`infrastructure.adapter.in.web`) son delgados y delegan a `UseCase`s; no se detectó lógica de negocio significativa en ellos.
- El dominio (`domain`) no contiene dependencias a Spring/JPA/HTTP (inspeccionado en modelos y excepciones). Correcto.
- No se retornan entidades JPA desde controladores; los controladores devuelven DTOs (`*Response`). Correcto.

## Mejoras opcionales

- Mover la anotación Spring fuera de la capa `application` (ver `AppointmentBookingPolicy`).
- Añadir una sección en `README.md` con instrucciones para ejecutar tests que requieren Docker/Testcontainers (ej. cómo habilitar Docker, variables de entorno o usar `-DskipITs` si se decide separar integracion). Esto reduciría confusión por el test skipped.
- Registrar en `docs/architecture.md` la decisión sobre beans de aplicación (si se acepta `@Component` en capa aplicación o no).
- Añadir un pequeño `CONTRIBUTING.md` con checklist: no usar `--no-verify`, correr `mvnw verify` antes de push, asegurarse de pruebas integracion locales si Docker está disponible.

## Deuda técnica pendiente

- Testcontainers: localmente la fábrica de Docker no encontró entorno válido (NPIPE on Windows). Actualmente el pipeline falla/skip en entornos sin Docker. Prioridad media: documentar y/o automatizar un fallback para CI.
- Mockito muestra advertencia sobre inline-mock-maker y su uso futuro como agente. Evaluar añadir configuración para Mockito native agent en el build si se requiere compatibilidad futura.
- `AppointmentBookingPolicy` acoplamiento a Spring (ver arriba). Si se desea estricta separación hexagonal, refactor menor requerido.

## Riesgos antes de producción

- Si el entorno de CI no tiene Docker configurado, algunos tests de integración pueden fallar o saltarse, lo que reduce la cobertura de verificación en entorno real.
- Pequeña posibilidad de conflicto si se forza `--no-verify` en commits (ya se evitó en HU-11), hay que mantener política de pre-push/pre-commit en el equipo.

## Recomendaciones para la siguiente iteración

- Documentar en `README.md` y `docs/architecture.md` la decisión sobre beans en la capa de aplicación y la guía para ejecutar tests con Testcontainers en Windows.
- Crear un pequeño script de verificación local que corra `mvnw verify` y chequeos estáticos antes de push (pre-push hook recomendado, opcionalmente con `husky` o similar para otros stacks).
- Priorizar un ticket para cerrar la deuda Testcontainers (documentar variables, o CI config) si se usa en entrevistas/CI.

## Archivos generados/modificados durante la auditoría

- `docs/quality-audit.md` (este archivo)
