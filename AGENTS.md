# AGENTS.md

## Idioma y estilo

- Responder en español.
- Ser preciso, directo y técnico.
- No improvisar cambios fuera del alcance solicitado.
- Antes de modificar código, revisar rama, estado Git y objetivo de la tarea.
- Si hay ambigüedad técnica relevante, reportar riesgo antes de implementar.

## Reglas de Git

- No trabajar directamente sobre `main`, `master`, `develop`, `qa` ni ramas protegidas.
- Crear ramas feature desde `develop` actualizado.
- No usar `git pull origin develop` desde una rama feature.
- No hacer push sin autorización explícita.
- No abrir PR sin autorización explícita.
- No hacer deploy sin autorización explícita.
- No hacer merge a `main` sin autorización explícita.
- No usar `--force` ni `--force-with-lease` sin autorización explícita.
- No commitear archivos locales de herramientas como `.codex/`.

Flujo base para crear rama:

```powershell
git switch develop
git pull origin develop
git status --short
git switch -c feature/HU-XX-descripcion
```

## Stack base

* Java 21.
* Spring Boot 3.
* Maven Wrapper.
* PostgreSQL.
* Flyway.
* Docker Compose.
* JUnit 5.
* Mockito.
* Testcontainers.
* Arquitectura hexagonal pragmática.

## Comandos de validación

Windows:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd test
git diff --check
git status --short
git log --oneline --decorate --graph --all -n 70
```

Linux/macOS:

```bash
./mvnw verify
./mvnw test
git diff --check
git status --short
git log --oneline --decorate --graph --all -n 70
```

Docker:

```powershell
docker compose up -d
docker compose ps
docker compose logs
```

Preferir `docker compose` sobre `docker-compose`.

## Estructura

* `src/main/java/.../domain`: modelos y reglas puras. Sin Spring, JPA, HTTP ni PostgreSQL.
* `src/main/java/.../application`: casos de uso, servicios de aplicación, commands, responses y ports.
* `src/main/java/.../infrastructure`: controllers REST, JPA entities, repositories, adapters, mappers, security, Swagger y configuración técnica.
* `src/main/resources/db/migration`: migraciones Flyway con formato `V{numero}__descripcion.sql`.

## Arquitectura

* Mantener arquitectura hexagonal.
* No poner lógica de negocio en controllers.
* No devolver entidades JPA desde controllers.
* No contaminar `domain` con dependencias técnicas.
* Evitar duplicar reglas entre booking administrativo, walk-in y booking público.
* Toda validación sensible debe vivir en domain/application o en constraints de base de datos, no solo en controller.

## Multi-tenant y seguridad

* `Company` representa la barbería/empresa.
* `Branch` representa una sede.
* En endpoints administrativos, el tenant se resuelve desde JWT.
* `X-Company-Id` y `X-Branch-Id` son fallback temporal solo si no hay JWT.
* Si hay JWT válido, los headers nunca deben sobrescribir el tenant del usuario.
* En endpoints públicos, el tenant se resuelve por `companySlug + branchSlug`.
* No aceptar `companyId`, `branchId`, `customerId` ni `endAt` en bodies públicos.
* No exponer `passwordHash`, roles internos, userAccountId, emails internos, datos de otros clientes ni ids internos de tenant si no son necesarios.

## Roles

Roles actuales:

* `PLATFORM_OWNER`
* `COMPANY_OWNER`
* `BRANCH_MANAGER`
* `RECEPTIONIST`
* `BARBER`
* `CASHIER`
* `ACCOUNTANT`
* `INVENTORY_MANAGER`
* `CUSTOMER`

Reglas principales:

* `BARBER` debe estar vinculado a `barberId`.
* `BARBER` solo puede operar agenda, dashboard, availability y citas propias.
* `CUSTOMER` no puede usar endpoints administrativos.
* `COMPANY_OWNER` no debe crear `PLATFORM_OWNER`.
* `BRANCH_MANAGER` no debe operar fuera de su branch.
* Roles futuros como `CASHIER`, `ACCOUNTANT` e `INVENTORY_MANAGER` no deben recibir permisos amplios hasta implementar sus módulos.

## Zonas de alto riesgo

### Booking y disponibilidad

Cuidar especialmente:

* Solapes de citas.
* Horarios laborales del barbero.
* Estados bloqueantes: `SCHEDULED`, `IN_PROGRESS`.
* Estados no bloqueantes: `CANCELLED`, `COMPLETED`, `NO_SHOW`.
* Cálculo de `endAt`.
* Validación de company, branch, customer, barber y service.

### Booking público

El booking público:

* No requiere login.
* Resuelve tenant por `companySlug + branchSlug`.
* Crea citas con `source = ONLINE`.
* Crea citas con `status = SCHEDULED`.
* Reutiliza o crea customer por `company + phone`.
* No debe revelar si un customer ya existía.
* No debe devolver datos almacenados de un customer preexistente si el request público envió otros datos.

### Idempotencia

Para endpoints públicos de creación:

* `Idempotency-Key` debe evitar doble creación por doble click o reintento.
* Misma key + mismo payload no debe crear otra cita.
* Misma key + payload diferente debe responder `409`.
* La idempotencia debe estar scopeada por tenant.
* Una key de un tenant no puede apuntar a appointment de otro tenant.
* Si una operación falla por regla de negocio, la key no debe quedar bloqueando al cliente indefinidamente.

### Concurrencia

Tener especial cuidado con:

* Creación simultánea de customer por el mismo `company + phone`.
* Creación simultánea de appointments con la misma idempotency key.
* Doble reserva del mismo slot.
* Constraints únicos en PostgreSQL.
* Transacciones.
* Locks o manejo explícito de conflictos si aplica.

No resolver concurrencia solo con validaciones en memoria o en aplicación. Preferir constraints de base de datos más manejo explícito de excepción.

### Migraciones

* Nunca modificar migraciones Flyway ya aplicadas.
* Crear una nueva migración para cada cambio.
* Validar migración limpia desde V1 hasta latest cuando sea posible.
* Validar migración sobre base existente cuando sea posible.
* FKs tenant-aware deben evitar mezclar datos entre companies/branches.
* Revisar especialmente FKs compuestas con `MATCH SIMPLE` en PostgreSQL.

## Tests requeridos

Toda HU debe incluir pruebas proporcionales al riesgo.

* Lógica de dominio nueva: unit tests.
* Servicios de aplicación: unit tests o integración según el caso.
* Endpoints REST: controller/security tests.
* Persistencia o migraciones: adapter/repository tests y PostgreSQL real cuando Docker esté disponible.
* Concurrencia/idempotencia: tests específicos de duplicación, conflicto y reintento.
* Seguridad: endpoints administrativos sin JWT deben responder `401`; roles no autorizados deben responder `403`.

## Validación obligatoria antes de reportar listo

Ejecutar:

```powershell
.\mvnw.cmd verify
git diff --check
git status --short
git log --oneline --decorate --graph --all -n 70
```

Si Docker/Testcontainers no está disponible:

* Decirlo claramente.
* No inventar que PostgreSQL real pasó.
* Reportar qué validación quedó pendiente.

## Reporte final obligatorio

Todo reporte final debe incluir:

* Rama actual.
* Commit creado.
* Migración Flyway creada si aplica.
* Endpoints afectados.
* Cambios de seguridad.
* Tests agregados/modificados.
* Resultado de `mvnw verify`.
* Resultado de `git diff --check`.
* Resultado de `git status --short`.
* Resultado de PostgreSQL real si se ejecutó.
* Riesgos/deuda técnica.
* Recomendación: auditar, mergear o no mergear.

## Prohibiciones

* No tocar `main` sin autorización.
* No hacer deploy sin autorización.
* No hacer push sin autorización.
* No abrir PR sin autorización.
* No subir ramas feature sin autorización.
* No imprimir secretos.
* No commitear `.env`.
* No modificar migraciones existentes.
* No mezclar refactors grandes con una HU funcional pequeña.
* No cambiar arquitectura sin justificarlo.
* No marcar como listo si los tests fallan.
