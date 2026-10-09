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

## Archivos y contratos principales

Backend: `application/service/CapabilityService`, `MarketplaceService`, `PasswordSecurityService`, sus puertos/DTOs, adaptadores JDBC, `PublicBarberShopPersistenceAdapter`, `JwtTokenAdapter`, filtros/configuración de seguridad y tests relacionados. El dominio conserva independencia técnica.

Frontend: `features/marketplace`, `features/account-security`, panel operativo, landing, facturación, rutas, servicio de autenticación y navegación. No se añadieron dependencias ni se modificó `environment.prod.ts`.

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
| Frontend | 41 pruebas en nueve suites; `npm run build` PASS |
| Dependencias runtime frontend | `npm audit --omit=dev --audit-level=high`: cero vulnerabilidades |
| HTTP real local | Health UP; catálogo 200; capacidades sin JWT 401; alta 201; login 200; FREE/DRAFT; cuentas equipo 402; cambio 204; JWT anterior 401; nuevo login 200 |
| Interfaz local | Landing/directorio vacío y navegación a login verificados en navegador; no se cargaron datos reales de clientes |
| Whitespace | `git diff --check` PASS en ambos repositorios |

Las diez pruebas omitidas localmente requieren Docker/Testcontainers; la CI en Ubuntu debe ejecutar `mvnw verify` con Docker. Los resultados anteriores de CI no prueban estos nuevos commits: comprobar el run asociado a la publicación de esta entrega.

No se borró la base local del usuario, no se reescribió historial, no hubo merge ni se alteraron secretos de producción. La QA utiliza una instancia/base aislada y datos sintéticos.

## Despliegue y límites pendientes

La aplicación arranca localmente. Neon contiene el proyecto propio de GHS; Render continúa en preparación del nuevo servicio API, con Docker, rama feature y plan Free. Un formulario configurado no demuestra despliegue ni conexión cloud.

La contraseña inicial del superadministrador y los secretos finales deben introducirse mediante el flujo autorizado del proveedor; después se deben comprobar health/readiness, login, aprobación de ficha, reserva y revisión de transferencia en HTTPS real. Retirar la contraseña de aprovisionamiento tras el primer arranque.

Confirmación de correo, recuperación por email, Google OAuth, MFA, cuotas comerciales avanzadas y almacenamiento de imágenes propio siguen pendientes. No se ha comprobado un abono bancario real; únicamente se verificó la lógica de aprobación. La aplicación no debe venderse como disponibilidad permanente garantizada sobre planes Free.

**Recomendación:** auditar los commits y ejecutar un piloto controlado después de la CI y validación cloud. No mergear ni anunciar producción completa por el resultado local. La referencia Antana y las decisiones de seguridad están en [su revisión](security-antana-reference-review.md).
