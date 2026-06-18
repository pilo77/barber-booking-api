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
- HU-18 implementa RBAC base por rol/path. No implementa todavia autorizacion
  fina por ownership de cada recurso.
- La siguiente HU obligatoria de seguridad es `HU-19: Authorization hardening
  and ownership rules`, antes de perfil publico, reservas por slug, caja, pagos
  o inventario.
- Los casos de uso deben consultar recursos dentro del tenant actual. Si un
  cliente, barbero, servicio u appointment existe en otra company/branch, debe
  tratarse como no encontrado para el tenant actual.
- La disponibilidad y el dashboard diario no deben mezclar citas de otra
  company o branch.
- Los nombres de servicios solo son unicos dentro de la misma company.

## Roles

- `PLATFORM_OWNER`: rol global de plataforma. Queda reservado para administracion
  SaaS y debe auditarse antes de uso operativo amplio.
- `COMPANY_OWNER`: administra usuarios y operacion de su tenant.
- `BRANCH_MANAGER`: consulta usuarios de su branch y opera la sucursal.
- `RECEPTIONIST`: gestiona clientes, citas, walk-ins y disponibilidad.
- `BARBER`: acceso operativo minimo a agenda/citas; la relacion user-barber se
  refinara en una HU futura.
- `CASHIER`, `ACCOUNTANT`, `INVENTORY_MANAGER`: reservados para caja, reportes e
  inventario futuros; no reciben acceso administrativo amplio todavia.
- `CUSTOMER`: reservado para portal publico/futuro; no puede usar endpoints
  administrativos actuales.

### Pendientes de authorization hardening

- Crear relacion formal `user_account -> barber`.
- Limitar `BARBER` a su propia agenda, dashboard y citas asignadas.
- Limitar `BRANCH_MANAGER` a operaciones de su branch.
- Limitar `RECEPTIONIST` segun permisos operativos concretos.
- Evitar acceso amplio a modulos no necesarios por rol.
- Agregar issuer, audience y `jti` al JWT antes de produccion.
- Registrar auditoria persistente de acciones sensibles.

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
