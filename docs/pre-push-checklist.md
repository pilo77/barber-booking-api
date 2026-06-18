# Pre-push Readiness Checklist

Estado del proyecto (resumen): funcionalidades implementadas, documentación y
auditoría añadidas (HU-11, HU-12, HU-13). Objetivo: validar antes de hacer
push/PR a remoto.

---

## Archivos que NO deben subirse
- `target/` (artefactos y evidencias de build)
- `.env` y cualquier archivo con credenciales reales
- `logs/` y archivos temporales (`*.log`, `tmp/`, `dumps/`)
- claves privadas y certificados (`*.pem`, `*.key`, `*.p12`, `*.jks`)

## Archivos que SÍ deben subirse
- `.env.example` (placeholders únicamente)
- `README.md`
- `docs/` (architecture, api-contract, business-rules, quality-audit, technical-debt, git-workflow, pre-push-checklist.md)
- `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`
- `docker-compose.yml`
- migraciones Flyway (`src/main/resources/db/migration`)
- código fuente y tests (`src/main/java`, `src/test/java`)

## Checklist de documentación
- `README.md` incluye: descripción, stack, comandos de ejecución, comandos de test, Docker Compose y Swagger UI referencia.
- `docs/` contiene los archivos requeridos: `architecture.md`, `api-contract.md`, `business-rules.md`, `quality-audit.md`, `technical-debt.md`, `git-workflow.md`, `pre-push-checklist.md`.

## Validaciones a realizar localmente
1. Ejecutar build y tests:

```powershell
.\mvnw.cmd verify
```

2. Revisar problemas de diff:

```powershell
git diff --check
git status --short
```

3. Escaneo rápido de secretos en archivos trackeados (patrones comunes).

## Riesgos pendientes
- Testcontainers/Docker en Windows: algunas pruebas de integración pueden saltarse si no hay Docker disponible. Documentado en `docs/technical-debt.md`.
- Advertencia Mockito inline-mock-maker: documentada como deuda técnica.
- `AppointmentBookingPolicy` y servicios de `application` usan anotaciones Spring por pragmatismo — documentado en `docs/architecture.md`.

## Recomendación final
- Tras confirmar las validaciones locales y revisar este checklist: hacer push y abrir PR desde `develop` o desde ramas feature según flujo. Asegurarse de no incluir `.env` ni artefactos en el commit.
