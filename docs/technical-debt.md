# Technical Debt & Decisions

Este documento resume hallazgos pequeños y seguros identificados en la
auditoría técnica (HU-12) y las decisiones tomadas para la versión actual.

## Resumen

- **Testcontainers / Docker (Windows NPIPE):** algunas pruebas de integración
  que dependen de Testcontainers se saltan o fallan si no existe un entorno
  Docker válido en Windows (NPIPE). Recomendación: documentar cómo ejecutar
  integraciones en Windows, y en CI habilitar un runner con Docker o usar
  fallback mocks para entornos sin Docker.

- **Mockito inline mock maker warning:** las pruebas muestran una advertencia
  indicando que Mockito se auto-adjunta. Esto funciona hoy pero requiere que se
  revise la estrategia de agente/inline-mock-maker si se actualiza la JVM en el
  futuro. Recomendación: documentar la advertencia y, si se desea, agregar el
  agente de Mockito en el build (opcional).

- **Uso pragmático de anotaciones Spring en `application`:** varias clases de
  la capa de `application` (p. ej. `AppointmentBookingPolicy`, servicios con
  `@Service`) están anotadas con Spring para facilitar inyección y pruebas.
  Esto es deliberado y aceptable en este proyecto; sin embargo no debe
  presentarse como "100% puro" hexagonal en entrevistas.

## Decisiones tomadas

1. No se refactoriza ahora `AppointmentBookingPolicy` ni otros `@Service` en
   `application`. En su lugar se documenta la decisión arquitectónica.
2. Documentar la advertencia de Mockito y el problema de Testcontainers como
   deuda técnica con baja prioridad pero visible para futuros mantenedores.
3. No tocar migraciones Flyway ni el archivo `PruebaTecnicaTrinity`.

## Posibles pasos futuros (opcionales)

- Refactor: convertir los servicios de `application` en POJOs y registrar
  beans desde `infrastructure` mediante clases `@Configuration` para una
  separación más estricta.
- Añadir documentación en `README.md` sobre cómo ejecutar tests con Docker en
  Windows y cómo configurar CI para que ejecute las pruebas de integración.
- Reemplazar los headers temporales `X-Company-Id` y `X-Branch-Id` por tenant
  derivado del contexto de seguridad/JWT en HU-18. Estado: implementado como
  prioridad principal. Los headers siguen existiendo como fallback temporal
  cuando no hay usuario autenticado.
- Limitar rol `BARBER` a su propio barbero requiere una relacion formal
  `user_account -> barber`; queda pendiente para una HU futura.
- `COMPANY_OWNER` actualmente crea usuarios en el tenant/branch derivado del
  JWT. La gestion completa multi-branch de usuarios dentro de una company queda
  pendiente.

## HU-18 Auth/RBAC limitations

HU-18 implementa una base funcional de autenticacion y RBAC, pero no debe
tratarse como autorizacion final de produccion. La autorizacion actual combina
roles y paths de Spring Security con validaciones de tenant en casos de uso.
Esto sirve como foundation, pero todavia no implementa ownership authorization
fina por recurso, operacion y relacion del usuario con la entidad operativa.

Limitaciones documentadas antes de merge:

- RBAC actual es por rol/path, no por ownership completo.
- Falta una relacion formal `user_account -> barber`.
- `BARBER` todavia no queda limitado estrictamente a su propia agenda,
  dashboard y citas.
- `BRANCH_MANAGER` requiere reglas finas por operacion dentro de su branch.
- `RECEPTIONIST` requiere permisos operativos mas precisos por modulo.
- Algunos roles internos tienen acceso amplio a modulos que no siempre
  necesitan para su responsabilidad final.
- JWT todavia no incluye validacion de issuer, audience ni `jti`; debe
  endurecerse antes de produccion.
- Falta auditoria persistente de acciones sensibles como bootstrap, login
  fallido repetido, creacion/desactivacion de usuarios y cambios de roles.

Decision: crear como siguiente HU obligatoria `HU-19: Authorization hardening
and ownership rules` antes de construir perfil publico, caja, pagos o
inventario.

## Referencias

- `docs/quality-audit.md` — hallazgos originales.
- `docs/architecture.md` — decisión arquitectónica actualizada.
# Technical Debt

## TECH-DEBT-01: Testcontainers on Windows Docker pipe

`mvn verify` currently builds successfully and runs unit/controller tests, but
the Spring context test using Testcontainers is skipped when Java cannot resolve
the active Docker Desktop pipe.

Observed environment:

- Docker CLI can use the `desktop-linux` context.
- Testcontainers attempts to use a pipe that returns an invalid/empty Docker
  response from Java.

Impact:

- HU-01 and HU-02 are still validated with unit/controller tests.
- Full database-backed integration tests should be enabled before booking logic
  in HU-06, where consistency and overlap rules become critical.

Next action:

- Configure Testcontainers to resolve Docker Desktop's active Windows pipe or
  run integration tests through a stable WSL/Linux Docker context.
