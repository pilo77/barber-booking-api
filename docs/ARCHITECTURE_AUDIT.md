# Architecture Audit and Production Readiness

## 1. Resumen ejecutivo

`barberia-ghs` tiene una base técnica sólida para seguir evolucionando como producto: backend Spring Boot con arquitectura hexagonal pragmática, PostgreSQL, Flyway, JWT, pruebas automatizadas y CORS configurable. El frontend Angular ya puede conectarse a la API real para autenticación local.

El estado actual todavía no debe considerarse listo para producción. El principal gap funcional es el módulo financiero: pagos, caja, comisiones y reportes. También quedan riesgos de seguridad y operación que deben cerrarse antes de exponer datos reales o financieros.

## 2. Estado técnico actual

- Backend local validado en `http://localhost:8080`.
- Health check disponible en `/actuator/health`.
- Swagger disponible en `/swagger-ui/index.html`.
- PostgreSQL Docker validado usando `localhost:5433` porque `5432` está ocupado por PostgreSQL local.
- Frontend local validado en `http://localhost:4200`.
- Frontend configurado para usar API real en desarrollo local.
- Autenticación real validada contra `/api/v1/auth/login` y `/api/v1/auth/me`.

## 3. Tecnologías detectadas

- Java 21.
- Spring Boot 3.
- Spring Security.
- JWT.
- Maven Wrapper.
- PostgreSQL.
- Flyway.
- Docker Compose.
- JUnit 5.
- Mockito.
- Testcontainers.
- Angular.
- TypeScript.
- npm.

## 4. Arquitectura actual

El backend sigue una arquitectura hexagonal pragmática:

- `domain`: modelos, value objects y reglas puras.
- `application`: casos de uso, comandos, respuestas y puertos.
- `infrastructure`: controladores REST, seguridad, persistencia, mappers, configuración y adaptadores técnicos.
- `src/main/resources/db/migration`: migraciones Flyway versionadas.

El frontend tiene separación por features, servicios compartidos y environments, pero aún conserva mocks y estado local en flujos operativos.

## 5. Arquitectura recomendada

- Mantener la arquitectura hexagonal en backend.
- Mantener lógica de negocio fuera de controllers.
- Formalizar tenant resolution como mecanismo central y no temporal.
- Migrar el frontend por fases hacia API real, empezando por flujos críticos.
- Mantener mocks solo como fixtures de test o story/demo data, no como fuente operativa.
- Usar contratos API explícitos para auth, agenda, reservas, pagos y reportes.
- Introducir auditoría persistente para acciones críticas.

## 6. Módulos existentes

- Autenticación y RBAC base.
- JWT y tenant desde token.
- Clientes.
- Barberos.
- Servicios ofrecidos.
- Horarios laborales.
- Disponibilidad.
- Citas administrativas.
- Citas walk-in.
- Booking público.
- Idempotencia para booking público.
- Perfil público y branding.
- Health checks y Swagger.

## 7. Módulos faltantes

- Pagos.
- Caja.
- Comisiones.
- Reportes financieros.
- Auditoría persistente.
- Refresh tokens y revocación de sesión.
- Gestión formal de sesiones.
- Interceptor frontend para `401` y `403`.
- Guards frontend conectados a permisos reales.
- Observabilidad operacional.

## 8. Riesgos críticos de seguridad

- JWT almacenado en `localStorage` en frontend.
- `roleGuard` pendiente de integración completa con autorización real.
- Falta interceptor HTTP para manejar `401` y `403`.
- JWT backend debe endurecerse con issuer, audience, jti, refresh y revocación.
- `SecurityConfig` debe evolucionar hacia `denyAll` por defecto en lugar de un catch-all amplio.
- El filtro temporal de tenant debe formalizarse y eliminar fallbacks inseguros cuando el flujo JWT esté completo.
- No se debe inventar una URL de producción en `environment.prod.ts`.

## 9. Riesgos de escalabilidad

- El dashboard todavía depende de mocks/localStorage en partes operativas.
- Sin auditoría persistente será difícil rastrear acciones críticas.
- Sin módulo financiero formal, pagos y caja quedarían expuestos a inconsistencias manuales.
- Falta estrategia explícita para métricas, trazas y logs operativos.
- Las pruebas con Testcontainers deben estabilizarse en el entorno local/CI para validar PostgreSQL real de forma consistente.

## 10. Riesgos multi-tenant

- Los endpoints administrativos deben resolver tenant desde JWT.
- Los headers `X-Company-Id` y `X-Branch-Id` deben quedar solo como fallback temporal.
- En endpoints públicos, el tenant debe derivarse desde `companySlug` y `branchSlug`.
- No se deben aceptar `companyId`, `branchId`, `customerId` ni `endAt` en bodies públicos.
- Las futuras tablas financieras deben tener constraints tenant-aware.

## 11. Protección de datos financieros

- No guardar datos financieros sensibles sin modelo y permisos claros.
- No exponer montos, caja, comisiones o reportes entre tenants.
- Registrar auditoría de cambios financieros.
- Separar permisos de caja, contabilidad y administración.
- Evitar logs con información financiera sensible.
- Validar idempotencia en pagos y operaciones de caja.

## 12. Checklist de producción

- Configurar `APP_CORS_ALLOWED_ORIGINS` con dominios reales, sin `*`.
- Configurar `APP_JWT_SECRET` seguro y rotación planificada.
- Definir issuer/audience/jti para JWT.
- Implementar refresh/revocación de sesiones.
- Reemplazar fallbacks temporales de tenant.
- Mover autorización por defecto hacia `denyAll`.
- Validar Flyway desde cero y sobre base existente.
- Validar PostgreSQL real en CI.
- Configurar logs sin secretos.
- Definir backups y restauración de PostgreSQL.
- Completar `environment.prod.ts` solo con URL real aprobada.
- Validar frontend con API real en ambiente de staging.

## 13. Plan por fases

1. Cierre local y documentación de estado actual.
2. Hardening de seguridad backend y frontend.
3. Migración del dashboard a API real.
4. Formalización multi-tenant sin fallbacks temporales.
5. Diseño e implementación del módulo financiero.
6. Auditoría persistente y reportes.
7. Validación de producción, CI y observabilidad.

## 14. Primeras 10 tareas técnicas recomendadas

1. Cambiar `SecurityConfig` hacia una política `denyAll` por defecto.
2. Agregar issuer, audience y jti a JWT.
3. Diseñar refresh tokens y revocación.
4. Reemplazar JWT en `localStorage` por una estrategia de sesión más segura.
5. Conectar `roleGuard` a permisos reales.
6. Agregar interceptor frontend para `401` y `403`.
7. Migrar dashboard desde mocks/localStorage hacia API real.
8. Diseñar módulo financiero antes de implementar pagos, caja y comisiones.
9. Agregar auditoría persistente para acciones críticas.
10. Estabilizar Testcontainers en CI y entorno local.

## 15. Qué no se debe tocar todavía

- No modificar migraciones Flyway existentes.
- No inventar URL de producción para `environment.prod.ts`.
- No implementar pagos, caja, comisiones ni reportes sin diseño técnico previo.
- No eliminar fallbacks de tenant sin validar todos los consumidores.
- No conectar dashboard completo a API real sin revisar contratos y datos necesarios.
- No hacer cambios masivos de arquitectura sin una decisión documentada.
