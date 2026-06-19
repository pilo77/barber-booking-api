# Business Rules

Este documento centraliza las reglas de negocio aplicadas por la API.

## Entidades y borrado lógico

- `Customer`, `Barber` y `ServiceOffering` usan soft delete: los registros no
  se borran fisicamente; se marcan con `active=false`.
- Las operaciones y validaciones deben comprobar `active=true` para considerar
  recursos como disponibles.

## Multi-tenant and Auth/RBAC

- `Company` representa la barberia/empresa propietaria de los datos.
- `Branch` representa una sede de una company.
- `Customer` y `ServiceOffering` pertenecen a una company.
- `Barber`, `BarberWorkingHour` y `Appointment` pertenecen a una company y una
  branch.
- Ningun request body debe enviar `companyId` ni `branchId`.
- Desde HU-18, en endpoints administrativos el tenant se resuelve primero desde
  JWT. Los headers temporales `X-Company-Id` y `X-Branch-Id` solo aplican si no
  hay usuario autenticado.
- Si existe JWT valido, los headers no pueden sobrescribir el tenant del
  usuario.
- HU-19 agrega hardening de autorizacion por ownership para agenda,
  dashboard, availability, lifecycle de citas y user accounts.
- Los casos de uso deben consultar recursos dentro del tenant actual. Si un
  cliente, barbero, servicio u appointment existe en otra company/branch, debe
  tratarse como no encontrado para el tenant actual.
- Los usuarios con rol `BARBER` deben estar vinculados a un `barberId`. Sin
  ese vinculo no pueden operar agenda propia.
- La disponibilidad y el dashboard diario no deben mezclar citas de otra
  company o branch.
- Los nombres de servicios solo son unicos dentro de la misma company.

## Roles

- `PLATFORM_OWNER`: rol global de plataforma. Queda reservado para administracion
  SaaS y debe auditarse antes de uso operativo amplio.
- `COMPANY_OWNER`: administra usuarios y operacion de su tenant.
- `BRANCH_MANAGER`: consulta usuarios de su branch y opera la sucursal.
- `RECEPTIONIST`: gestiona clientes, citas, walk-ins y disponibilidad dentro
  del tenant actual. No gestiona usuarios ni roles.
- `BARBER`: consulta su agenda, dashboard y disponibilidad propia; puede
  iniciar, completar, cancelar o marcar no-show solo en sus propias citas.
- `CASHIER`, `ACCOUNTANT`, `INVENTORY_MANAGER`: reservados para caja, reportes e
  inventario futuros; no reciben acceso administrativo amplio todavia.
- `CUSTOMER`: reservado para portal publico/futuro; no puede usar endpoints
  administrativos actuales.

### Authorization hardening

- Existe relacion formal `user_accounts.barber_id -> barbers`.
- Un `barberId` solo puede vincularse a un usuario y debe pertenecer a la misma
  company/branch.
- `BARBER` no puede ver agenda ni dashboard de otro barbero.
- `BARBER` no puede operar citas de otro barbero.
- `BARBER` no puede gestionar customers, barbers, services ni user accounts.
- `CUSTOMER` no puede usar endpoints administrativos actuales.
- Roles `CASHIER`, `ACCOUNTANT` e `INVENTORY_MANAGER` quedan reservados para
  modulos futuros y no deben recibir acceso operativo amplio todavia.

Pendientes:

- Agregar issuer, audience y `jti` al JWT antes de produccion.
- Registrar auditoria persistente de acciones sensibles.
- Gestion multi-branch completa para `COMPANY_OWNER` requiere endpoints de
  administracion de branches.

## Perfil publico de barberia

- HU-20 expone consultas publicas por slug y HU-21 agrega disponibilidad y
  reserva publica sin login.
- Los endpoints publicos resuelven el tenant por `companySlug` y, para sedes,
  por `companySlug + branchSlug`.
- `branch.slug` se trata como unico dentro de una company, no globalmente.
- Los endpoints publicos no dependen de `X-Company-Id`, `X-Branch-Id` ni JWT.
- Solo una company activa puede tener perfil publico visible.
- Solo branches activas se listan o consultan publicamente.
- Solo services activos con `visible_for_online_booking=true` se listan
  publicamente.
- El endpoint de servicios públicos de branch valida que la branch pertenece a la
  company pública, pero devuelve el catálogo visible de servicios de esa company.
  No existe un catálogo de servicios específico por branch en HU-20.
- Solo barbers activos con `active_for_online_booking=true` se listan
  publicamente.
- La respuesta publica puede exponer `service.id` y `barber.id` para preparar
  HU-21 de disponibilidad/reserva, pero no expone `companyId`, `branchId`,
  emails internos, telefonos de barberos, usuarios, roles ni credenciales.
- Los servicios siguen siendo de company. Mientras no exista una relacion
  service-branch, el listado publico de servicios de una branch devuelve los
  servicios visibles de la company a la que pertenece esa branch.

### Reserva publica

- La reserva publica resuelve `companyId` y `branchId` solo desde
  `companySlug + branchSlug`; ignora headers temporales y no acepta ids de
  tenant en el body.
- Company y branch deben estar activas. La branch debe pertenecer a la company.
- Service debe pertenecer a la company, estar activo y ser visible online.
- Barber debe pertenecer a la company y branch resueltas, estar activo y ser
  visible online.
- El customer se busca por telefono dentro de la company. Si existe y esta
  activo se reutiliza; si no existe se crea. No se acepta `customerId` publico.
- La respuesta usa nombre y telefono normalizados del request; nunca devuelve
  datos almacenados del customer para indicar directa o indirectamente si ya
  existia.
- Toda cita publica se crea con `source = ONLINE` y `status = SCHEDULED`.
- `startAt` debe ser futuro; `endAt` se calcula en backend.
- Horarios laborales, solapes y estados bloqueantes se validan mediante la
  politica compartida de booking.
- No se exponen credenciales, ids internos de tenant, roles ni datos de otros
  clientes.
- HU-21 no incluye pagos, cancelacion publica ni reprogramacion publica.
- La creacion concurrente del mismo `company + phone` puede producir un
  `409 Conflict` por la restriccion unica y queda como deuda tecnica conocida.

## Reglas de disponibilidad

- Un `Barber` solo puede recibir citas si `barber.active == true`.
- Un `Customer` solo puede reservar si `customer.active == true`.
- Un `ServiceOffering` solo puede usarse si `serviceOffering.active == true`.

## Cálculo de `endAt`

- `endAt` siempre se calcula en backend usando `startAt` + `durationMinutes`
  del `ServiceOffering` asociado.
- El frontend NO debe enviar `endAt`; los contratos de reserva no lo aceptan.

## Regla de solape (overlap)

Se considera que una cita existente `existing` y una nueva `new` se solapan si:

```
existing.startAt < newEndAt && existing.endAt > newStartAt
```

## Estados que bloquean disponibilidad

- Bloqueantes: `SCHEDULED`, `IN_PROGRESS`.
- No bloqueantes: `CANCELLED`, `COMPLETED`, `NO_SHOW`.

## Transiciones válidas de estado

- `SCHEDULED -> IN_PROGRESS`
- `IN_PROGRESS -> COMPLETED`
- `SCHEDULED -> CANCELLED`
- `SCHEDULED -> NO_SHOW`

Las transiciones invalidas deben responder con `409 Conflict` y un código de
error explicito.

## Diferencia entre `ONLINE` y `WALK_IN`

- `source = ONLINE`: reserva hecha por frontend/cliente remoto.
- `source = WALK_IN`: cliente atendido en el local.

### Walk-in inmediato vs agendado

- Walk-in inmediato: `source = WALK_IN` y `status = IN_PROGRESS` (se inicia al
  momento de la creacion).
- Walk-in agendado: `source = WALK_IN` y `status = SCHEDULED` (se reserva para
  un horario posterior).

## Disponibilidad por slots

- Los `slots` se calculan dinamicamente a partir de: horarios laborales activos
  del barbero, duracion del servicio y citas del dia.
- No se persisten slots en la base de datos; se generan en tiempo de consulta.

## Dashboard diario

- El dashboard diario resume: totales por estado, `nextAppointment` y
  `occupiedMinutes`.
- `occupiedMinutes` suma minutos de `SCHEDULED`, `IN_PROGRESS` y `COMPLETED`.

## Observaciones

- Validar siempre `active` de recursos antes de crear o iniciar una cita.
- Centralizar validaciones en `AppointmentBookingPolicy` para evitar
  duplicacion entre booking administrativo, publico y walk-ins.
