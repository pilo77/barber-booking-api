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

## Reglas de disponibilidad

- Un `Barber` solo puede recibir citas si `barber.active == true`.
- Un `Customer` solo puede reservar si `customer.active == true`.
- Un `ServiceOffering` solo puede usarse si `serviceOffering.active == true`.

## Cálculo de `endAt`

- `endAt` siempre se calcula en backend usando `startAt` + `durationMinutes`
  del `ServiceOffering` asociado.
- El frontend NO debe enviar `endAt`; si viene, el servidor lo ignora y lo
  recalcula.

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
  duplicacion entre endpoints `appointments` y `walk-ins`.
