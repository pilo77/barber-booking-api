# API Contract

Version base de endpoints planeados. La implementacion debe avanzar por
historias de usuario pequenas y testeadas.

## Customers

```http
POST   /api/v1/customers
GET    /api/v1/customers
GET    /api/v1/customers/{id}
PUT    /api/v1/customers/{id}
PATCH  /api/v1/customers/{id}/deactivate
```

Request `POST /api/v1/customers`:

```json
{
  "fullName": "Ana Perez",
  "phone": "3001234567",
  "email": "ana@example.com"
}
```

Response:

```json
{
  "id": 1,
  "fullName": "Ana Perez",
  "phone": "3001234567",
  "email": "ana@example.com",
  "active": true,
  "createdAt": "2026-06-17T12:00:00Z",
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

## Barbers

```http
POST   /api/v1/barbers
GET    /api/v1/barbers
GET    /api/v1/barbers/{id}
PUT    /api/v1/barbers/{id}
PATCH  /api/v1/barbers/{id}/activate
PATCH  /api/v1/barbers/{id}/deactivate
```

Request `POST /api/v1/barbers`:

```json
{
  "fullName": "Carlos Gomez",
  "phone": "3101234567",
  "email": "carlos@example.com"
}
```

Response:

```json
{
  "id": 1,
  "fullName": "Carlos Gomez",
  "phone": "3101234567",
  "email": "carlos@example.com",
  "active": true,
  "createdAt": "2026-06-17T12:00:00Z",
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

## Barber Working Hours

```http
POST   /api/v1/barbers/{barberId}/working-hours
GET    /api/v1/barbers/{barberId}/working-hours
GET    /api/v1/barbers/{barberId}/working-hours/{workingHourId}
PUT    /api/v1/barbers/{barberId}/working-hours/{workingHourId}
PATCH  /api/v1/barbers/{barberId}/working-hours/{workingHourId}/activate
PATCH  /api/v1/barbers/{barberId}/working-hours/{workingHourId}/deactivate
```

Request `POST /api/v1/barbers/{barberId}/working-hours`:

```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "08:00:00",
  "endTime": "12:00:00"
}
```

Response:

```json
{
  "id": 1,
  "barberId": 1,
  "dayOfWeek": "MONDAY",
  "startTime": "08:00:00",
  "endTime": "12:00:00",
  "active": true,
  "createdAt": "2026-06-17T12:00:00Z",
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

Los horarios no se borran fisicamente. La desactivacion se realiza con
`PATCH /api/v1/barbers/{barberId}/working-hours/{workingHourId}/deactivate`.
Los cruces entre horarios activos del mismo barbero y dia responden `409 Conflict`.

## Services

```http
POST   /api/v1/services
GET    /api/v1/services
GET    /api/v1/services/{id}
PUT    /api/v1/services/{id}
PATCH  /api/v1/services/{id}/activate
PATCH  /api/v1/services/{id}/deactivate
```

Request `POST /api/v1/services`:

```json
{
  "name": "Corte clasico",
  "description": "Corte tradicional",
  "durationMinutes": 30,
  "price": 25000.00
}
```

Response:

```json
{
  "id": 1,
  "name": "Corte clasico",
  "description": "Corte tradicional",
  "durationMinutes": 30,
  "price": 25000.00,
  "active": true,
  "createdAt": "2026-06-17T12:00:00Z",
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

El endpoint no expone borrado fisico. La desactivacion se realiza con
`PATCH /api/v1/services/{id}/deactivate` y conserva el registro historico.

## Appointments

```http
POST   /api/v1/appointments
GET    /api/v1/appointments/{id}
GET    /api/v1/barbers/{barberId}/appointments?date=2026-06-17
PATCH  /api/v1/appointments/{id}/cancel
```

Request `POST /api/v1/appointments`:

```json
{
  "customerId": 1,
  "barberId": 1,
  "serviceOfferingId": 1,
  "startAt": "2026-06-18T09:00:00"
}
```

Response:

```json
{
  "id": 1,
  "customerId": 1,
  "barberId": 1,
  "serviceOfferingId": 1,
  "startAt": "2026-06-18T09:00:00",
  "endAt": "2026-06-18T09:30:00",
  "status": "SCHEDULED",
  "source": "ONLINE",
  "createdAt": "2026-06-17T18:45:00Z",
  "updatedAt": null
}
```

El backend calcula `endAt` con la duracion del servicio. Las citas nuevas
inician como `SCHEDULED` y `ONLINE`. Los cruces con citas activas o reservas
fuera del horario laboral activo del barbero responden `409 Conflict`.

## Availability

```http
GET /api/v1/barbers/{barberId}/availability?date=2026-06-17&serviceOfferingId=1
```

Respuesta esperada:

```json
{
  "barberId": 1,
  "serviceOfferingId": 1,
  "date": "2026-06-17",
  "slots": [
    {
      "startAt": "2026-06-17T08:00:00",
      "endAt": "2026-06-17T08:30:00",
      "status": "AVAILABLE",
      "color": "GREEN",
      "available": true
    },
    {
      "startAt": "2026-06-17T08:30:00",
      "endAt": "2026-06-17T09:00:00",
      "status": "OCCUPIED",
      "color": "RED",
      "available": false
    }
  ]
}
```

La disponibilidad se calcula dinamicamente con los horarios laborales activos
del barbero, la duracion del servicio y las citas del dia. Solo bloquean
`SCHEDULED` e `IN_PROGRESS`; `CANCELLED`, `COMPLETED` y `NO_SHOW` no bloquean.
El paso de generacion de slots se configura con `BOOKING_SLOT_STEP_MINUTES`
o `booking.slot-step-minutes` y por defecto es de 15 minutos.

## Barber Daily Dashboard

```http
GET /api/v1/barbers/{barberId}/daily-dashboard?date=2026-06-17
```

Respuesta esperada:

```json
{
  "barberId": 1,
  "barberName": "Carlos Gomez",
  "date": "2026-06-17",
  "summary": {
    "totalAppointments": 2,
    "scheduled": 1,
    "inProgress": 0,
    "completed": 0,
    "cancelled": 1,
    "noShow": 0,
    "occupiedMinutes": 30
  },
  "nextAppointment": {
    "appointmentId": 1,
    "customerId": 1,
    "customerName": "Ana Perez",
    "serviceOfferingId": 1,
    "serviceName": "Corte clasico",
    "startAt": "2026-06-17T09:00:00",
    "endAt": "2026-06-17T09:30:00",
    "status": "SCHEDULED",
    "source": "ONLINE"
  },
  "appointments": []
}
```

El dashboard lista solo citas del barbero en la fecha consultada, ordenadas por
`startAt`. `occupiedMinutes` suma `SCHEDULED`, `IN_PROGRESS` y `COMPLETED`;
`CANCELLED` y `NO_SHOW` no cuentan como ocupacion. `nextAppointment` toma la
primera cita `SCHEDULED` o `IN_PROGRESS` que aun no haya terminado segun la hora
actual del servidor.
