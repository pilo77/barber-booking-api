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
- **Catálogo público de servicios por branch:** HU-20 valida que la branch
  pertenece a la company pública, pero devuelve el catálogo visible de servicios
  de esa company. La implementación de un catálogo de servicios específico por
  branch queda para una HU futura.

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
- Limitar rol `BARBER` a su propio barbero quedo cubierto en HU-19 mediante
  `user_accounts.barber_id`.
- `COMPANY_OWNER` actualmente crea usuarios en el tenant/branch derivado del
  JWT. La gestion completa multi-branch de usuarios dentro de una company queda
  pendiente.

## HU-22 public booking anti-abuse limitations

- El rate limiter de booking publico vive en memoria y se aplica por instancia.
  Reiniciar o escalar horizontalmente el backend reinicia o divide las cuotas.
- La IP usada es `HttpServletRequest.getRemoteAddr()`. No se procesa
  `X-Forwarded-For` hasta definir una lista/configuracion de proxies confiables.
- Antes de exposicion publica de alto trafico, mover cuotas a Redis, API Gateway
  o WAF y evaluar captcha/challenge adaptativo.
- Las filas idempotentes no tienen limpieza automatica todavia. Definir una
  retencion y un job seguro antes de que el volumen sea significativo.
- Dos keys distintas concurrentes para un customer nuevo con el mismo telefono
  todavia pueden competir por la restriccion unica de customer y devolver 409.

## HU-18/HU-19 Auth/RBAC limitations

HU-18 implemento una base funcional de autenticacion y RBAC. HU-19 agrego
hardening de ownership para los flujos actuales mas sensibles: user accounts,
agenda, dashboard, availability y lifecycle de citas. Esto mejora la seguridad
operativa, pero todavia no debe tratarse como autorizacion final completa de
produccion para modulos futuros.

Limitaciones documentadas antes de merge:

- Spring Security sigue siendo una primera barrera por rol/path; las reglas de
  ownership viven en application services/policies.
- `BRANCH_MANAGER` queda protegido por tenant/branch actual, pero la gestion
  multi-branch completa requiere endpoints de administracion de branches.
- `RECEPTIONIST` queda restringido fuera de user accounts y modulos de gestion,
  pero futuras operaciones de caja/pagos/inventario deben definir permisos
  propios.
- JWT todavia no incluye validacion de issuer, audience ni `jti`; debe
  endurecerse antes de produccion.
- Falta auditoria persistente de acciones sensibles como bootstrap, login
  fallido repetido, creacion/desactivacion de usuarios y cambios de roles.

Decision: despues de auditar HU-19, el siguiente bloque funcional puede avanzar
hacia perfil publico, manteniendo issuer/audience/jti y auditoria persistente
como hardening previo a produccion.

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
