# Business Rules

Este documento centraliza las reglas de negocio aplicadas por la API.

## Entidades y borrado lógico

- `Customer`, `Barber` y `ServiceOffering` usan soft delete: los registros no
  se borran fisicamente; se marcan con `active=false`.
- Las operaciones y validaciones deben comprobar `active=true` para considerar
  recursos como disponibles.

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
