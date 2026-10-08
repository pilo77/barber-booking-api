# Barbería GHS: revisión y validación del 8 de octubre de 2026

## Resultado

Los flujos principales funcionan en un ambiente local aislado con PostgreSQL real.
Se implementaron registro de barbería, suscripción mensual de **$40.000 COP**,
reporte de transferencia, revisión por `PLATFORM_OWNER`, activación del mes,
panel operativo y reserva pública conectados al backend. Se conservó la arquitectura
hexagonal pragmática. **No se ha desplegado ni validado en producción.**

El ambiente local usa únicamente datos y movimientos bancarios ficticios. Ninguna
prueba realizó una transferencia real ni verificó ingresos en la cuenta del propietario.
No se contrató un plan de pago ni se compró un dominio.

## Arquitectura y archivos principales

| Área | Cambios y propósito |
| --- | --- |
| Dominio | `BillingOrder`, `CompanySubscription`: importe de la orden, método de pago y vigencia. Dominio sin dependencias Spring/JPA/HTTP. |
| Aplicación | `CompanyOnboardingService`, `ManualBillingService`, `BillingService`, `PlatformOwnerProvisioningService` y puertos: reglas, permisos y transacciones fuera de los controllers. |
| Persistencia | Adaptadores JDBC de alta, suscripción, reportes y administrador; locks PostgreSQL, claves tenant-aware y auditoría de aprobación/rechazo. |
| Citas | `AppointmentRepositoryPort`, repositorio JPA y servicios de ciclo de vida: lock por cita/tenant para serializar transiciones; reloj de Colombia para el dashboard. |
| Seguridad | `SecurityConfig`, filtros JWT/abuso/suscripción, hasher BCrypt, política de usuarios y configuración segura de producción. |
| Frontend | Nuevos componentes `saas`, `billing`, `operations`, `public-booking`; modelos y contratos reales, prioridad de roles y protección ante respuestas tardías de otra sesión/sede. |
| Cloud | Dockerfile, ambos `render.yaml`, perfil `application-prod.yml`, build cloud Angular, workflows y documentación de variables. |

Los componentes legacy de demostración se conservan fuera de las rutas operativas
actuales. Las pantallas nuevas no reemplazan una respuesta fallida por citas inventadas.
`environment.prod.ts` permanece sin modificar; la compilación cloud usa una configuración
separada y exige la URL HTTPS real de la API.

La prueba `HexagonalArchitectureTest` protege la independencia técnica del dominio.
Los servicios de aplicación utilizan transacciones y anotaciones Spring, conforme a la
arquitectura pragmática existente; no se presenta como arquitectura purista completa.

## Flujo de suscripción y administrador

1. Registro público: crea empresa, sede principal y propietario con plan pendiente.
2. Login del propietario: permite acceder a su plan y reportar la transferencia.
3. La orden toma el precio del servidor. El navegador no define el importe a pagar.
4. El reporte registra empresa, usuario autenticado y correo del remitente; permanece
   `PENDING`. Un correo o una captura no prueban pago ni propiedad del correo.
5. El administrador consulta pendientes, coteja el movimiento en su banco e introduce
   identificador bancario e importe exacto. Puede aprobar o rechazar con motivo.
6. La aprobación concede un mes calendario; renovación anticipada conserva la vigencia
   restante. Reintentos no duplican meses y una transacción bancaria no paga dos órdenes.
7. Sin plan activo, las operaciones y nuevas reservas se bloquean. Login y pago siguen
   disponibles para el propietario. No existe débito recurrente automático.

El aprovisionamiento del primer administrador se hace por variables secretas de arranque,
con lock transaccional y BCrypt. No promueve un usuario existente ni reemplaza su contraseña.
El registro público nunca crea `PLATFORM_OWNER`. Después del primer arranque se desactiva
la bandera y se elimina la contraseña inicial del ambiente.

## Endpoints añadidos y afectados

| Ruta bajo `/api/v1` | Uso |
| --- | --- |
| `POST /auth/register-company` | Registro público de empresa/sede/propietario pendiente. |
| `GET /company/context` | Contexto del usuario dentro de su tenant. |
| `GET /billing/subscription` | Tarifa y vigencia del plan del propietario. |
| `GET /billing/transfer-instructions` | Instrucciones configuradas por ambiente. |
| `GET, POST /billing/transfers` | Reportes propios y creación idempotente. |
| `GET /platform/billing/transfers` | Pendientes para el administrador de plataforma. |
| `PATCH /platform/billing/transfers/{reference}/approve` | Aprobación con referencia bancaria única e importe exacto. |
| `PATCH /platform/billing/transfers/{reference}/reject` | Rechazo auditado sin activar acceso. |
| `/auth/login`, `/auth/me`, endpoints administrativos y públicos | Validación de sesión/permisos; plan obligatorio para operación y nuevas reservas. |
| `/appointments/{id}/{start,complete,cancel,no-show}` | Transiciones protegidas por tenant y lock de persistencia. |

Se dejó preparado el adaptador opcional Wompi (`/billing/checkout`, consulta de órdenes
y `/webhooks/wompi`), con firma y confirmación del proveedor. **Está deshabilitado**;
no se validó con una cuenta comercial ni se anuncia cobro automático. Los canales MANUAL
y WOMPI están separados para impedir que un webhook apruebe una orden manual.

## Seguridad comprobada y límites

| Control | Resultado local |
| --- | --- |
| Roles | Prueba parametrizada con los nueve roles reales y JWT: permisos explícitos, facturación propia y administración exclusiva de plataforma. |
| Tenant | Headers no sobrescriben el tenant autenticado; datos de otra empresa no aparecen. BARBER restringido a su agenda y citas propias. |
| JWT | Emisor/audiencia, expiración y revalidación de cuenta/roles/tenant. Desactivar una cuenta invalida un token ya emitido. |
| Contraseñas | Nuevas cuentas requieren al menos 12 caracteres y máximo 72 bytes UTF-8 para BCrypt; comparación dummy en usuario inexistente. |
| Acceso | Administrativos sin JWT: 401; rol no permitido: 403; plan pendiente: 402 en operación. Denegación por defecto de módulos desconocidos. |
| Abuso | Límite acotado de intentos de login/alta y reserva pública por instancia. Reserva pública mantiene como máximo 10.000 claves, elimina ventanas vencidas y rechaza claves nuevas si la capacidad activa está completa. No es un limitador distribuido. |
| Proxy | Tomcat ignora cabeceras de peers no confiables y resuelve clientes distintos detrás de proxies internos. Cuatro pruebas de la válvula real pasaron; la cadena de Render todavía requiere validación cloud. |
| Producción | CORS HTTPS explícito, TLS PostgreSQL `verify-full`, validación del certificado/nombre, bootstrap público cerrado, Swagger deshabilitado y health sin detalles. |
| Navegador | Sesión en `sessionStorage`; no se envía JWT a otros orígenes ni reservas públicas; respuestas tardías no restauran o cierran otra sesión. |

**No están implementados Google OAuth, verificación/OTP por correo, recuperación de
contraseña ni MFA.** Google en Render o Neon únicamente sirve para administrar infraestructura.
El login de GHS usa correo y contraseña. El control de pago es independiente de la
verificación de correo y se basa en revisión bancaria del administrador.

La sesión en `sessionStorage` sigue expuesta ante un XSS ejecutado en el origen. No se
ha demostrado resistencia a una auditoría de penetración completa ni carga de producción.
El contacto de privacidad/soporte, recuperación segura y protección reforzada del
administrador deben resolverse antes de abrir ventas generales.

## Migraciones

- V15: suscripciones y órdenes con importe e idempotencia.
- V16: revisión manual, método, estados y auditoría con referencia bancaria única.
- V17: usuario que reporta la transferencia y FK compuesta por empresa.

No se modificaron migraciones antiguas. Flyway V1–V17 arrancó en bases PostgreSQL nuevas
aisladas. Se probaron constraints, método cruzado, usuario de otro tenant y concurrencia.
No se migró una base de producción existente ni se ejecutó una migración destructiva.

## Evidencia de pruebas

| Validación | Resultado |
| --- | --- |
| `mvnw.cmd verify` local | BUILD SUCCESS; 408 reportadas, 398 ejecutadas, 10 omitidas; cero fallos/errores. Incluye las pruebas nuevas de capacidad/concurrencia del limitador y de proxy. |
| PostgreSQL real | 26 casos de la suite contra PostgreSQL 18.2 local: seguridad (14), suscripción (4), revisión manual (3), administrador (3), ciclo de citas (2). Están incluidos en las 408 pruebas reportadas. |
| Docker/Testcontainers | Motor Docker no disponible localmente; 10 casos omitidos localmente. La CI remota con Docker/PostgreSQL 16 ejecutó la suite completa: 408 pruebas, cero fallos/errores y cero omitidas. |
| CI backend | [Run 37837500234](https://github.com/pilo77/barber-booking-api/actions/runs/37837500234), commit funcional `4da87ab`: Maven verify y build correctos. |
| CI frontend | [Run 37835566293](https://github.com/pilo77/barberia-ghs-frontend/actions/runs/37835566293), commit `2dbc408`: pruebas, auditoría y builds correctos. |
| `npm test -- --watch=false` | 21 pruebas aprobadas en 5 archivos. Contrato `available` de disponibilidad real incluido. |
| `npm run build` | Correcto en Angular con configuración normal. |
| `npm run build:cloud` | Correcto con URL `.invalid` solo para compilación; no prueba conexión cloud. |
| `npm audit --audit-level=high` | Cero vulnerabilidades reportadas por npm en esta ejecución. No sustituye auditoría completa. |
| `git diff --check` | Correcto en ambos repositorios; avisos de normalización CRLF/LF sin errores de whitespace. |
| Lint | No existe un script lint configurado; compilación TypeScript y pruebas sí ejecutadas. |
| Arranque y HTTP | Comprobados en QA local: readiness `UP`, registro 201, login real y suscripción con tarifa 4.000.000 centavos. La API se detuvo para empaquetar la última verificación; su reinicio fue rechazado por revisión automática de permisos y permanece detenida. |
| Navegador | Superadministrador ficticio aprueba reporte ficticio; propietario entra; reserva pública confirmada con id 1. La API cambia SCHEDULED → IN_PROGRESS → COMPLETED y una lectura posterior confirma persistencia. |
| Móvil | Agenda revisada a 390×844, sin desbordamiento horizontal de la página; tabla desplazable dentro de su contenedor. |

Evidencia visual local sin datos de clientes reales en `target/production-evidence/`:
`superadmin-local.png`, `public-booking-local.png`, `agenda-mobile-local.png`,
`render-repository-access.png`, `render-github-two-repos-approval.png`,
`render-static-created.png`, `render-spa-rewrite.png`, `render-security-headers.png` y
`render-api-admin-handoff.png`. Último log de
verificación local en `target/deployment-verify.log`; evidencia anterior en
`target/production-verify.log` y logs
ignorados del frontend. Estos artefactos son locales y no se versionan.

## Estado cloud y bloqueos concretos

Se confirmó sesión en Render y proyecto Neon Free `barberia-ghs`, PostgreSQL 16,
Virginia. No se alteraron servicios ni bases de RematePOS. Inicialmente Render solo
mostraba `RematePos/RematePos-Backend` como fuente existente. Se completó la instalación
GitHub de Render con acceso limitado a `pilo77/barber-booking-api` y
`pilo77/barberia-ghs-frontend`. El usuario autorizó ambas conexiones, completó la
revalidación de identidad de GitHub y Render muestra ambos repositorios como fuentes
desplegables. El frontend es privado.
Ambas ramas revisadas ya se publicaron con autorización y sus pipelines pasaron.
Se creó la Static Site `barberia-ghs`, ID `srv-db416pij9qps73fp580g`, con origen real
`https://barberia-ghs.onrender.com`, rama frontend revisada y Node 24. Se canceló el
primer build mientras falta la URL real del backend. Se guardaron rewrite SPA, cuatro
headers de seguridad y un header de caché; aún no se validó una publicación correcta del frontend.

La API está preparada en el formulario Render del usuario: Docker, Virginia, Free
($0/mes), rama backend revisada, autodeploy off y health `/actuator/health/readiness`.
La conexión directa Neon se cargó, tras autorización, en campos secretos de Render,
sin imprimirla ni guardarla en archivos; usa JDBC con `sslmode=verify-full` y
credenciales separadas. La API todavía no está creada: el usuario debe completar
sus datos de administrador, contraseña nueva, clave JWT y destino bancario, y enviar
el formulario. No se presume que una plantilla o una URL asignada esté desplegada.

Para completar la publicación faltan:

1. Completar `APP_JWT_SECRET` (Generate), `PLATFORM_OWNER_EMAIL`,
   `PLATFORM_OWNER_NAME`, `PLATFORM_OWNER_INITIAL_PASSWORD` y
   `BILLING_MANUAL_INSTRUCTIONS`, y enviar Deploy web service. La política del
   navegador exige que el usuario introduzca y envíe sus credenciales nuevas.
   No inferir correo ni titular bancario ni enviar contraseña por chat.
2. Obtener la URL real asignada a la API, configurar `GHS_API_URL` en la Static Site
   y ejecutar el build. No usar una URL inventada o local.
3. Confirmar creación y login del administrador; desactivar aprovisionamiento y
   eliminar la contraseña inicial de las variables después del primer arranque.
4. Ejecutar despliegues, migraciones y pruebas de health/login/pago/reserva en sus URLs
   reales. Las plantillas YAML no constituyen evidencia de despliegue.
5. Probar respaldo/restauración, recuperación de cuenta y requisitos operativos antes
   de vender como servicio plenamente disponible.

Render/Neon Free con subdominio del proveedor es una opción inicial de $0, sujeta a cuotas
y suspensión; no garantiza servicio permanente ni disponibilidad continua. Un dominio
propio requiere renovaciones. El procedimiento y fuentes de límites constan en
[el runbook](cloud-deployment-runbook.md).

## Control de versiones y recomendación

Trabajo aislado en `feature/production-readiness-billing` y
`feature/production-readiness-ui`. Se conserva el commit frontend previo `e848ca5`.
No se hizo merge, force push, eliminación de ramas ni modificación de una rama protegida.
Se publicaron ambas ramas y se comprobó su CI. Los commits funcionales comprobados son
`4da87ab` (backend) y `2dbc408` (frontend). Esta actualización documental no modifica
el comportamiento de la aplicación; el inventario final se obtiene con
`git log --oneline -n 6` y el estado de ambas ramas debe acompañar la entrega.

**Recomendación:** revisión del cambio y piloto controlado después de resolver los
bloqueos. No mergear ni anunciar esta entrega como producción completa todavía.
El precio de $40.000 es una hipótesis de lanzamiento sustentada en comparación comercial;
requiere validación con barberías y costos de soporte observados.
