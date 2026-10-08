# Despliegue inicial Render + Neon

Fecha de revisión: 2026-10-08. Plan BASIC: **$40.000 COP por mes calendario y empresa**.
El cobro autorizado es transferencia con verificación manual del ingreso. Wompi permanece
deshabilitado. Este procedimiento no implica que el despliegue real ya esté validado.

## Infraestructura y presupuesto

| Componente | Configuración inicial | Límites relevantes |
| --- | --- | --- |
| API | Render Web Service, Docker, Free, Virginia | 512 MB, 0,1 CPU; se suspende tras 15 minutos sin tráfico; despertar aproximado de un minuto. |
| Web | Render Static Site | Subdominio HTTPS `onrender.com`; consume cuota de transferencia y compilación del workspace. |
| Datos | Neon Free, PostgreSQL 16, AWS Virginia | 1 GB por proyecto, 100 CU-h/mes por proyecto, 10 branches, ventana de restauración de seis horas. |
| Dominio | Subdominio del proveedor | No exige comprar un dominio. Un dominio propio implica renovaciones y no está incluido. |

Render concede 750 horas gratuitas **compartidas por workspace**, no por aplicación. Los
servicios anteriores consumen esa misma cuota. No añadir tarjeta ni activar planes pagos
para mantener el presupuesto en $0: ante ciertos límites, los servicios o compilaciones
pueden suspenderse. Revisar Billing antes de cada despliegue; no modificar RematePOS.

Estas condiciones permiten un piloto comercial de presupuesto mínimo; **no ofrecen
disponibilidad garantizada ni permanencia de por vida**. Render desaconseja su instancia
Free para producción. No almacenar bases de datos, comprobantes o backups en el disco
efímero del contenedor. Tampoco usar Render Postgres Free: caduca a los 30 días.

Fuentes oficiales: [Render Free](https://render.com/docs/free),
[recursos de cómputo](https://render.com/docs/compute-plans),
[Neon Free actualizado el 2 de octubre de 2026](https://neon.com/blog/neon-free-plan-1-gb-per-project).

## Precondiciones de entrega

1. Revisar ramas y cambios locales de ambos repositorios; conservar cambios anteriores.
2. Ejecutar backend `mvnw verify`, frontend tests, build normal y build cloud, auditoría
   de dependencias y `git diff --check`. PostgreSQL real es obligatorio para facturación,
   migraciones, idempotencia y concurrencia. Documentar las omisiones si Docker no funciona.
3. Revisar los archivos antes de commitear. Nunca incluir `.env`, credenciales, datos de
   banco personales, logs, `target`, `dist`, `node_modules`, dumps ni backups.
4. Publicar solamente commits revisados con la autorización Git correspondiente. No
   desplegar una rama que no contenga los cambios y pruebas aprobados.
5. Registrar commit exacto, servicio, rama, región y variables por nombre, sin sus valores.

Los workflows existentes mantienen los jobs `Maven verify` y `Build Angular app`.
Se ejecutan en pushes a ramas y PRs a `develop`/`main`; incluyen las validaciones nuevas
sin workflows duplicados. El build cloud en CI utiliza una URL `.invalid` deliberadamente
no desplegable para comprobar la compilación; no representa un backend real.

## PostgreSQL y TLS

Usar el proyecto Neon propio `barberia-ghs`, no bases ni cuentas de otra aplicación.
Seleccionar la branch y base correctas. Para la primera migración se prefiere conexión
directa: las migraciones usan locks y DDL. La aplicación tiene un pool pequeño de tres
conexiones. Evitar mantener instancias despiertas mediante tráfico artificial: consume
la cuota gratuita.

La conexión JDBC debe cumplir este formato, **solo como plantilla**:

```text
jdbc:postgresql://<host-directo-de-neon>/<base>?sslmode=verify-full
```

El usuario y contraseña van en variables separadas, nunca en la URI. El perfil `prod`
configura `org.postgresql.ssl.DefaultJavaSSLFactory`: usa el almacén de autoridades
de confianza del JVM. Esto evita depender de un archivo `~/.postgresql/root.crt` ausente
en el contenedor. Mantener `verify-full` para validar certificado y nombre del servidor;
no sustituirlo por `require` ni por `NonValidatingFactory`.

La validación de arranque rechaza base local, credenciales dentro de la URL, TLS débil,
parámetros duplicados o que desactivan verificaciones, factory insegura, suscripción
deshabilitada, sandbox y CORS inválido. El mensaje de error no imprime valores sensibles.

Fuente: [TLS en pgJDBC](https://jdbc.postgresql.org/documentation/ssl/) y
[propiedades de conexión](https://jdbc.postgresql.org/documentation/use/).

Para una separación de privilegios posterior, usar un rol migrador propietario del esquema
y un rol de ejecución con permisos CRUD mínimos. Si se configura una conexión Flyway
independiente, también debe llevar TLS verificado y el factory JVM; las propiedades del
pool Hikari no se heredan por una conexión independiente. No declarar permisos mínimos
verificados hasta comprobar los grants en la base real.

## Variables del backend

Configurar en Render mediante campos secretos; no pegar sus valores en el chat ni en logs.

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod`. |
| `SPRING_DATASOURCE_URL` | JDBC con hostname/base Neon reales y `sslmode=verify-full`, sin credenciales. |
| `SPRING_DATASOURCE_USERNAME` | Rol de la aplicación en la base indicada. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña del rol, campo secreto. |
| `APP_JWT_SECRET` | Valor aleatorio fuerte generado por Render; mínimo 32 caracteres. |
| `APP_JWT_EXPIRATION_MINUTES` | Opcional; `15` por defecto en producción, máximo permitido 60. |
| `APP_CORS_ALLOWED_ORIGINS` | Origen HTTPS real de la web, sin slash final/path, wildcard ni credenciales. |
| `BILLING_MANUAL_INSTRUCTIONS` | Medio Bre-B/Nequi, llave indicada por el usuario, titular validado y pautas de referencia. No versionar datos personales. |
| `BILLING_BASIC_AMOUNT_IN_CENTS` | `4000000`; modifica solo órdenes nuevas. |
| `WOMPI_ENABLED` | Mantener `false` para transferencia manual. |
| `PLATFORM_OWNER_PROVISIONING_ENABLED` | `true` únicamente durante el primer arranque que crea el administrador; luego `false`. |
| `PLATFORM_OWNER_EMAIL` | Correo confirmado por el usuario para administrar la plataforma. |
| `PLATFORM_OWNER_NAME` | Nombre confirmado del administrador. |
| `PLATFORM_OWNER_INITIAL_PASSWORD` | Contraseña inicial ingresada por el usuario en campo secreto; eliminar después del primer arranque correcto. |
| `PORT` | Lo inyecta Render; no fijar un puerto local. |

`APP_BOOTSTRAP_TOKEN` no habilita el endpoint público bajo `prod`: el perfil lo deja vacío.
La web y API deben identificarse por las URLs que Render realmente asigna. Para romper
la dependencia inicial de CORS, crear primero la Static Site y verificar su origen antes
del primer arranque de la API; si todavía no puede compilar por falta de `GHS_API_URL`,
configurar esa variable y repetir el build cuando la URL real de la API esté disponible.
No sustituirla por una URL inventada ni publicar una web apuntando a `localhost`.

## Administrador de plataforma

La base nueva requiere un `PLATFORM_OWNER` antes de poder aprobar transferencias.
El registro público solo crea `COMPANY_OWNER` pendiente y no debe crear administradores.
El provisionador de arranque se activa solo con `PLATFORM_OWNER_PROVISIONING_ENABLED=true`.
Ingresar correo, nombre y contraseña en sus variables dedicadas del servicio Render.
El usuario introduce la contraseña de forma segura en un campo secreto, nunca en el chat
ni como argumento del shell. La contraseña debe cumplir la política vigente de la aplicación.
Arrancar la API y confirmar health y login. Después desactivar la bandera y eliminar
`PLATFORM_OWNER_INITIAL_PASSWORD`, guardar ambos cambios juntos y volver a validar health.

La operación usa el puerto de aplicación y un lock transaccional PostgreSQL: crea un
PLATFORM_OWNER sin empresa ni sede solo cuando todavía no existe. Nunca promociona un
correo ya vinculado a otro usuario y no reemplaza administradores ni contraseñas existentes.
No existe un endpoint público adicional para ese aprovisionamiento.

El titular real de la cuenta de cobro y el correo del administrador los confirma el usuario.
No inferir su identidad a partir de GitHub o nombres de perfiles. Sin ese aprovisionamiento
no se considera terminado el flujo de cobro. No habilitar un bootstrap público permanente
ni crear credenciales predeterminadas para resolverlo.

## Orden del despliegue

1. Confirmar que las branches publicadas y los commits corresponden a las revisiones.
2. Crear la Static Site vinculada a la rama `feature/production-readiness-ui`; obtener
   el origen real asignado. Runtime estático, build `npm ci && npm run build:cloud`,
   directorio `dist/barberia-ghs-frontend/browser`. Seleccionar Node 24 para coincidir con CI.
3. Crear API Docker Free en Virginia, rama `feature/production-readiness-billing`.
   Configurar variables obligatorias, TLS y origen frontend exacto. Mantener autodeploy off.
4. Revisar logs de arranque reales, aplicación de Flyway V1–V17 y health
   `/actuator/health/readiness`. Una base preexistente debe validarse antes de migrar:
   no editar migraciones ya aplicadas y no ejecutar operaciones destructivas. La revisión
   actual añade V17 para la trazabilidad del usuario que reporta la transferencia;
   comprobar el historial completo.
5. Aprovisionar administrador mediante el procedimiento seguro y confirmar su acceso,
   sin exponer token ni contraseña. Comprobar que el bootstrap público permanezca cerrado.
6. Obtener URL HTTPS real de la API y configurar en la web `GHS_API_URL` con `/api/v1`
   al final. Recompilar. No es necesario editar `environment.prod.ts`.
7. La configuración pública `app-config.js` solo contiene la URL de API; nunca secretos.
   El build cloud la genera en `dist`; Render sirve ese archivo con `Cache-Control: no-store`.
8. Verificar rewrite SPA y headers de seguridad. Abrir `/login`, `/dashboard`, `/billing`
   y la ruta pública de reserva directamente, además de navegar desde la landing.

Los YAML son plantillas de infraestructura: [referencia oficial](https://render.com/docs/blueprint-spec).
Si se usa Blueprint, `sync: false` solicita los valores al crearlo; revisar cada servicio
antes de confirmar y no aplicar los nombres a servicios ajenos existentes.

## Validación funcional en el ambiente real

- Health y readiness devuelven `UP` sin detalles sensibles; Swagger y API docs no públicos.
- Login correcto; credenciales incorrectas sin distinguir usuario inexistente; sesión
  vencida devuelve 401; cuenta desactivada invalida el token existente.
- Administrativos sin JWT: 401. Roles no autorizados: 403. Tenant/branch del JWT prevalece
  sobre headers y body. BARBER solo consulta y opera su agenda.
- Registrar barbería crea empresa/sede/propietario con plan pendiente. Operación y nuevas
  reservas se bloquean mientras el plan no esté activo; el propietario puede renovar.
- Reportar transferencia no concede acceso. Solo PLATFORM_OWNER revisa el ingreso real,
  confirma referencia bancaria e importe de la orden y aprueba o rechaza con trazabilidad.
- Aprobar dos veces no añade otro mes; una transacción bancaria no paga dos órdenes.
  Un importe inferior al de la orden se rechaza. Una orden antigua conserva su precio.
- Con plan activo: crear servicio/barbero/horario/cliente, reservar públicamente,
  comprobar solape y doble click, operar start/complete/cancel/no-show y walk-in.
- Probar los nueve roles; no presentar caja, contabilidad o inventario futuros como
  módulos terminados. Revisar estados de carga/error y vista móvil sin datos demo.
- No validar dinero real usando una captura como prueba ni inventar una transferencia:
  el administrador coteja en su banco y registra la operación real.

Guardar evidencia sanitizada: commit, fecha, entorno, casos y códigos HTTP, nunca JWT,
headers de autorización, información de clientes reales ni valores secretos.

## Operación, respaldos y límites pendientes

La ventana de restauración gratuita de Neon no reemplaza un backup verificado. Antes
de captar clientes, definir exportación cifrada fuera del contenedor, retención y ensayo
de restauración. Los dumps no se versionan ni se adjuntan al chat.

Medir memoria y arranque en el contenedor real: heap al 65% no limita toda la memoria
del proceso. Si aparecen reinicios/OOM, corregir y validar; no anunciar estabilidad solo
porque el build funciona. Vigilar cuotas existentes de Render y Neon y planificar el
paso a infraestructura con disponibilidad y backups apropiados si aumenta el uso.

La seguridad actual no incluye recuperación de contraseña, verificación de correo ni
MFA. El límite de abuso por instancia no es distribuido. Estas exclusiones y las cuotas
de alojamiento deben comunicarse; no describir el producto como 100% seguro o ilimitado.

El perfil `prod` usa `server.forward-headers-strategy: native`, con la válvula de Tomcat
y sus restricciones predeterminadas no vacías para proxies internos; no configurar
`internal-proxies` vacío, `trusted-proxies` universal ni estrategia `framework`.
La válvula ignora cabeceras de un peer no confiable y recorre `X-Forwarded-For` desde
la derecha hasta el primer salto no confiable, conservando la separación entre clientes
cuando los saltos intermedios son internos. La cadena efectiva de Render puede incluir
otros proxies: antes de anunciar el límite por IP validado en nube, comprobar con tráfico
controlado de clientes diferentes que la dirección resuelta corresponde al cliente y que
un prefijo falsificado no la altera. No ampliar confianza a rangos públicos sin evidencia
del proveedor ni registrar direcciones o cabeceras completas de usuarios reales.
Fuentes: [Spring Boot 3.5 sobre proxies](https://docs.spring.io/spring-boot/3.5/how-to/webserver.html)
y [Render sobre IP del cliente](https://render.com/articles/how-render-handles-ddos-attacks).

Un rollback del código solo se autoriza si es compatible con el esquema ya migrado.
No revertir la base mediante SQL destructivo ni resetear Flyway. Ante falla, identificar
servicio/commit y error sanitizado, corregir la causa, repetir health y flujo crítico.
