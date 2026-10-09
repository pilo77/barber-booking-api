# Entrega: directorio moderado y seguridad de cuentas

Fecha: 2026-10-08. Ramas de trabajo: backend `feature/production-readiness-billing` y frontend `feature/production-readiness-ui`.

## Comportamiento entregado

- Registro de propietario con operación básica gratuita; la ficha nueva empieza como borrador.
- Capacidades `FREE`/`BUSINESS`. Gestión empresarial de $40.000 COP/mes habilita administrar cuentas de equipo mediante el flujo existente de transferencia y aprobación manual. El vencimiento conserva operación básica y registros.
- Directorio público real con filtros, paginación y enlaces a reservas. Solo muestra perfiles aprobados y recursos activos reservables.
- Propietario edita, solicita revisión u oculta la ficha. Editar una ficha publicada la devuelve a borrador; el contenido no revisado deja de exponerse.
- Superadministrador aprueba/rechaza una versión concreta mediante `expectedUpdatedAt`. Una decisión obsoleta devuelve 409 y exige revisar de nuevo.
- Reservas de perfiles explícitos publicados y ocultamiento se serializan en PostgreSQL: la reserva conserva un lock compartido hasta confirmar; ocultar primero impide nuevas reservas.
- Cambio autenticado de contraseña con CAS e incremento transaccional de versión de sesión. Invalida JWT anteriores y evita login con un hash antiguo o restauración del hash por JPA.
- Controles de roles, ownership y suspensión independientes del flag comercial legado; respuestas tardías del cliente no restablecen otra sesión ni privilegios caducados.
- Nueve espacios privados por actor. `/dashboard` y `/account` consultan la sesión real y redirigen según el rol principal determinístico. Las rutas restringen menú y consultas por su actor, incluso para cuentas con varios roles.

| Actor | Ruta privada | Alcance |
| --- | --- | --- |
| `PLATFORM_OWNER` | `/platform` | Revisión de fichas y reportes de transferencia |
| `COMPANY_OWNER` | `/company/dashboard` | Administración de su barbería y Gestión empresarial |
| `BRANCH_MANAGER` | `/branch/dashboard` | Operación y equipo autorizado de su sede |
| `RECEPTIONIST` | `/reception/dashboard` | Agenda, citas y clientes de su sede |
| `BARBER` | `/barber/agenda` | Agenda vinculada y citas propias |
| `CUSTOMER` | `/customer` | Directorio público y seguridad de cuenta, sin historial personal simulado |
| `CASHIER` | `/cashier` | Cuenta/seguridad; módulo de caja pendiente |
| `ACCOUNTANT` | `/accounting` | Cuenta/seguridad; módulo de contabilidad pendiente |
| `INVENTORY_MANAGER` | `/inventory` | Cuenta/seguridad; módulo de inventario pendiente |

Cada destino verifica sus roles; el backend sigue autorizando cada operación y recurso. Cliente y módulos futuros no inician peticiones administrativas. La ruta `/account/security` permite cambiar únicamente la contraseña de la cuenta autenticada.

## Archivos y contratos principales

Backend: `application/service/CapabilityService`, `MarketplaceService`, `PasswordSecurityService`, sus puertos/DTOs, adaptadores JDBC, `PublicBarberShopPersistenceAdapter`, `JwtTokenAdapter`, filtros/configuración de seguridad y tests relacionados. El dominio conserva independencia técnica.

Frontend: `features/marketplace`, `features/account-security`, `features/account`, `dashboardRedirectGuard`, modelos de rol, panel operativo, landing, facturación, rutas, servicio de autenticación y navegación. No se añadieron dependencias ni se modificó `environment.prod.ts`.

| Endpoint | Acceso |
| --- | --- |
| `GET /api/v1/public/marketplace` | Público, campos de catálogo sin IDs internos ni emails |
| `GET /api/v1/company/capabilities` | Propietario, gerente, recepción y barbero del tenant |
| `GET/PUT /api/v1/company/marketplace-profile` | Propietario de la empresa/sede asignada |
| `POST /api/v1/company/marketplace-profile/submit` y `/hide` | Propietario |
| `GET /api/v1/platform/marketplace/submissions` | `PLATFORM_OWNER` |
| `PATCH /api/v1/platform/marketplace/submissions/{branchId}/approve` y `/reject` | `PLATFORM_OWNER`, snapshot obligatorio |
| `POST /api/v1/auth/change-password` | Cuenta autenticada, modifica únicamente su contraseña |

V18 crea perfiles/auditoría de publicación y V19 crea versiones persistidas de sesión. Son migraciones nuevas y aditivas; no se alteran las ya aplicadas. Perfiles legacy sin fila conservan su política previa de enlaces privados; no aparecen automáticamente en el directorio.

## Verificación local confirmada

| Control | Resultado |
| --- | --- |
| `mvnw.cmd verify` final | BUILD SUCCESS: 556 reportadas, 546 ejecutadas, cero fallos/errores, 10 omitidas por Docker local no disponible |
| PostgreSQL real aislado | 52 pruebas sobre PostgreSQL 18.2, incluyendo migración limpia V1–V19 y actualización desde V14 |
| Seguridad | Matriz de los nueve roles, tenant/branch/ownership, 401/403/402, suspensión con enforcement legado desactivado, revocación JWT |
| Concurrencia | Moderación obsoleta, ocultamiento/reserva, CAS de contraseñas, rollback y guardado JPA antiguo |
| Frontend final por actor | 68 pruebas en 12 suites; `npm run build` PASS; revisión independiente sin bloqueos P1/P2 |
| Dependencias runtime frontend | `npm audit --omit=dev --audit-level=high`: cero vulnerabilidades |
| HTTP real local | Health UP; catálogo 200; capacidades sin JWT 401; alta 201; login 200; FREE/DRAFT; cuentas equipo 402; cambio 204; JWT anterior 401; nuevo login 200 |
| Interfaz local | Landing/directorio vacío y navegación a login verificados en navegador; `/platform` sin sesión redirige a `/login?session=expired`; no se cargaron datos reales de clientes |
| Whitespace | `git diff --check` PASS en ambos repositorios |

Las diez pruebas omitidas localmente requieren Docker/Testcontainers. La [CI backend 37882002704](https://github.com/pilo77/barber-booking-api/actions/runs/37882002704) ejecutó `mvnw verify` sobre el commit exacto `2b7749d30692ed100ca244d5e4a0cefb5a338bd9`: **556 pruebas, cero fallos, cero errores, cero omitidas y BUILD SUCCESS**. La [CI frontend final 37883340139](https://github.com/pilo77/barberia-ghs-frontend/actions/runs/37883340139) pasó para `177e532397eafe0e1d63806d7d6c930f6411d5f5`: **68 pruebas en 12 suites**, build normal y cloud de prueba correctos, whitespace correcto y `npm audit` con cero vulnerabilidades.

No se borró la base local del usuario, no se reescribió historial, no hubo merge ni se alteraron secretos de producción. La QA utiliza una instancia/base aislada y datos sintéticos.

## Despliegue y límites pendientes

La aplicación arranca localmente. Neon contiene el proyecto propio de GHS; Render continúa en preparación del nuevo servicio API, con Docker, rama feature y plan Free. Un formulario configurado no demuestra despliegue ni conexión cloud.

El formulario de Render queda preparado con plan Free, Docker, la rama de trabajo, CORS del subdominio real, conexión JDBC con TLS verificado, puerto de la plataforma y health check `/actuator/health/readiness`. Autodeploy está desactivado. Quedan pendientes `APP_JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD` y `PLATFORM_OWNER_INITIAL_PASSWORD`; sus valores no se registran en este documento. La política del control de navegador exige que el usuario introduzca y envíe las credenciales nuevas.

La contraseña inicial del superadministrador y los secretos finales deben introducirse mediante el flujo autorizado del proveedor; después se deben comprobar health/readiness, login, aprobación de ficha, reserva y revisión de transferencia en HTTPS real. Configurar el frontend con la URL HTTPS efectivamente asignada a la API y reconstruirlo. Retirar la contraseña de aprovisionamiento tras el primer arranque.

Confirmación de correo, recuperación por email, Google OAuth, MFA, cuotas comerciales avanzadas y almacenamiento de imágenes propio siguen pendientes. No se ha comprobado un abono bancario real; únicamente se verificó la lógica de aprobación. La aplicación no debe venderse como disponibilidad permanente garantizada sobre planes Free.

**Recomendación:** los commits funcionales tienen CI aprobada y están disponibles para auditoría. Ejecutar un piloto controlado después de validar el entorno cloud. No mergear ni anunciar producción completa por el resultado local. La referencia Antana y las decisiones de seguridad están en [su revisión](security-antana-reference-review.md).
