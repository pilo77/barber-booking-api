# API Contract

Version base de endpoints planeados. La implementacion debe avanzar por
historias de usuario pequenas y testeadas.

## Swagger / OpenAPI

La aplicacion expone la especificacion OpenAPI y la UI de Swagger en:

- `/v3/api-docs` (JSON OpenAPI)
- `/swagger-ui/index.html` (Swagger UI)

Estos endpoints estan habilitados desde la configuracion en la capa
`infrastructure`.

## Temporary Tenant Headers

Desde HU-18, los endpoints administrativos usan JWT Bearer. El tenant real se
resuelve primero desde el usuario autenticado:

- `companyId`
- `branchId`
- `barberId` cuando el usuario interno esta vinculado a un barbero
- `roles`

Los headers temporales siguen disponibles solo como fallback de desarrollo o
testing cuando no hay usuario autenticado:

```http
X-Company-Id: 1
X-Branch-Id: 1
```

Reglas:

- Si hay JWT valido, los headers no pueden sobrescribir `companyId` ni
  `branchId`.
- Si no hay JWT y no se envian headers, la API usa la company y branch default
  (`1/1`).
- `companyId` y `branchId` no se envian en el body de requests.
- `X-Company-Id` scopea `customers` y `services`.
- `X-Company-Id` + `X-Branch-Id` scopean `barbers`, `working-hours`,
  `appointments`, `availability` y `daily-dashboard`.
- Esta estrategia de headers es temporal. Los endpoints publicos resuelven la
  barberia por `slug`, no por ids de tenant enviados por el cliente.

## Auth and RBAC

Endpoints publicos:

```http
POST /api/v1/auth/bootstrap
POST /api/v1/auth/login
GET  /api/v1/public/**
GET  /actuator/health
GET  /swagger-ui/**
GET  /v3/api-docs/**
```

`POST /api/v1/auth/bootstrap` solo crea el primer usuario si no existe ningun
registro en `user_accounts`. Requiere header:

```http
X-Bootstrap-Token: <APP_BOOTSTRAP_TOKEN>
```

Request:

```json
{
  "email": "owner@example.com",
  "password": "StrongPassword123!",
  "fullName": "Owner User"
}
```

El usuario inicial queda como `COMPANY_OWNER` del tenant default `1/1`. El
password se guarda con BCrypt y nunca se devuelve.

`POST /api/v1/auth/login`:

```json
{
  "email": "owner@example.com",
  "password": "StrongPassword123!"
}
```

Response:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "owner@example.com",
    "fullName": "Owner User",
    "companyId": 1,
    "branchId": 1,
    "barberId": null,
    "roles": ["COMPANY_OWNER"]
  }
}
```

Los endpoints protegidos deben enviar:

```http
Authorization: Bearer <accessToken>
```

`GET /api/v1/auth/me` devuelve el usuario autenticado.

## Public Barber Shop Profile

HU-20 expone consultas publicas sin JWT para mostrar una barberia, sus sedes,
servicios visibles y barberos visibles. HU-21 usa esos recursos para consultar
disponibilidad y crear citas publicas.

Endpoints:

```http
GET /api/v1/public/barber-shops/{companySlug}
GET /api/v1/public/barber-shops/{companySlug}/branches
GET /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}
GET /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}/services
GET /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}/barbers
```

Decision de slugs:

- `company.slug` es unico globalmente.
- `branch.slug` es unico dentro de cada company.
- Por eso los endpoints de branch usan `companySlug + branchSlug` para evitar
  colisiones entre barberias distintas.
- Estos endpoints no usan `X-Company-Id` ni `X-Branch-Id`.

`GET /api/v1/public/barber-shops/{companySlug}`:

```json
{
  "slug": "ponte-perro",
  "name": "Ponte Perro Barberia",
  "description": "Barberia especializada en cortes modernos.",
  "logoUrl": "https://cdn.example.com/logo.png",
  "active": true
}
```

`GET /api/v1/public/barber-shops/{companySlug}/branches`:

```json
[
  {
    "slug": "neiva-centro",
    "name": "Neiva Centro",
    "address": "Calle 1 #2-3",
    "phone": "3001234567"
  }
]
```

`GET /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}/services`:

```json
[
  {
    "id": 1,
    "name": "Corte clasico",
    "description": "Corte tradicional.",
    "durationMinutes": 30,
    "price": 25000.00
  }
]
```

`GET /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}/barbers`:

```json
[
  {
    "id": 1,
    "displayName": "Santiago",
    "photoUrl": "https://cdn.example.com/santiago.png",
    "bio": "Especialista en fade y barba.",
    "specialties": "Fade, barba, cejas"
  }
]
```

Reglas de exposicion:

- Solo se devuelven companies activas.
- Solo se devuelven branches activas.
- Solo se devuelven services activos y `visibleForOnlineBooking=true`.
- El endpoint de servicios públicos de branch valida que la branch pertenece a la
  company pública, pero devuelve el catálogo visible de servicios de esa company.
  No existe un catálogo de servicios específico por branch en HU-20.
- Solo se devuelven barbers activos y `activeForOnlineBooking=true`.
- Se pueden exponer `id` de service/barber porque HU-21 los usara para
  disponibilidad y reserva.
- No se exponen emails internos, telefonos de barberos, usuarios, roles,
  password hashes, `companyId` ni `branchId`.
- Si el recurso no existe o esta inactivo, se responde `404`.

## Public Availability and Booking

HU-21 permite consultar slots y reservar sin login. El tenant se resuelve
exclusivamente desde `companySlug + branchSlug`; los headers `X-Company-Id` y
`X-Branch-Id` se ignoran para rutas publicas.

```http
GET  /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}/barbers/{barberId}/availability?date=2026-06-20&serviceOfferingId=1
POST /api/v1/public/barber-shops/{companySlug}/branches/{branchSlug}/appointments
```

La disponibilidad devuelve el mismo formato de slots documentado en
`Availability`, pero solo si company, branch, service y barber son publicamente
reservables dentro del tenant resuelto.

Request de reserva publica:

```json
{
  "serviceOfferingId": 1,
  "barberId": 2,
  "startAt": "2026-06-20T10:00:00",
  "customer": {
    "fullName": "Carlos Villamil",
    "phone": "3001234567",
    "email": "cliente@example.com"
  }
}
```

`companyId`, `branchId`, `customerId` y `endAt` no se aceptan en el request
publico. `startAt` debe ser futuro y `endAt` se calcula con la duracion del
servicio.

Response `201 Created`:

```json
{
  "id": 10,
  "status": "SCHEDULED",
  "source": "ONLINE",
  "startAt": "2026-06-20T10:00:00",
  "endAt": "2026-06-20T10:30:00",
  "service": {
    "id": 1,
    "name": "Corte clasico",
    "durationMinutes": 30,
    "price": 25000.00
  },
  "barber": {
    "id": 2,
    "displayName": "Santiago",
    "photoUrl": null
  },
  "customer": {
    "fullName": "Carlos Villamil",
    "phone": "3001234567"
  }
}
```

Reglas:

- Company y branch deben existir, estar activas y corresponder a los slugs.
- Service debe pertenecer a la company, estar activo y visible online.
- Barber debe pertenecer a esa company/branch, estar activo y visible online.
- Customer se busca por `company + phone`; se reutiliza si esta activo o se
  crea dentro de la company. El cliente publico nunca envia `customerId` y la
  respuesta refleja los datos basicos enviados, sin revelar datos almacenados
  de un customer preexistente.
- Se reutilizan horarios laborales, estados bloqueantes y regla de solape de
  `AppointmentBookingPolicy`.
- Recurso no publicable: `404`. Horario invalido, solape o recurso interno
  inactivo: `409` segun el manejador existente.
- No hay pagos, cancelacion publica ni reprogramacion publica en HU-21.
- Los servicios siguen siendo company-wide. El catalogo por sede requiere una
  relacion futura `branch_services`.
- Dos solicitudes concurrentes para crear el mismo `company + phone` pueden
  competir por la restriccion unica; una puede finalizar en `409 Conflict`.

## User Accounts

Endpoints protegidos:

```http
POST  /api/v1/user-accounts
GET   /api/v1/user-accounts
GET   /api/v1/user-accounts/{id}
PATCH /api/v1/user-accounts/{id}/activate
PATCH /api/v1/user-accounts/{id}/deactivate
```

Request `POST /api/v1/user-accounts`:

```json
{
  "email": "reception@example.com",
  "password": "StrongPassword123!",
  "fullName": "Reception User",
  "phone": "3001234567",
  "branchId": 1,
  "roles": ["RECEPTIONIST"]
}
```

Para crear un usuario con rol `BARBER`, el body debe incluir `barberId`:

```json
{
  "email": "barber@example.com",
  "password": "StrongPassword123!",
  "fullName": "Barber User",
  "phone": "3001234568",
  "branchId": 1,
  "barberId": 5,
  "roles": ["BARBER"]
}
```

El body no acepta `companyId`; se deriva del JWT del usuario autenticado.
`branchId` es opcional y, si se envia, debe pertenecer a la company del usuario
actual. `BRANCH_MANAGER` solo puede crear usuarios dentro de su branch.
`barberId`, cuando se envia, debe pertenecer a la misma company/branch y solo
puede usarse para usuarios con rol `BARBER`. `COMPANY_OWNER` puede crear roles
operativos internos, pero no `PLATFORM_OWNER`, `COMPANY_OWNER` ni `CUSTOMER`
desde este endpoint.

Responses de usuario y `GET /api/v1/auth/me` incluyen `barberId` y nunca
incluyen `passwordHash`.

Reglas de ownership principales:

- `BARBER` solo puede consultar su agenda, dashboard, availability y operar
  citas asociadas a su propio `barberId`.
- `BARBER` no puede listar customers ni gestionar barbers, services o user
  accounts.
- `RECEPTIONIST` puede operar clientes y citas dentro del tenant actual, pero
  no gestionar user accounts.
- `BRANCH_MANAGER` queda limitado por tenant/branch y no puede asignar roles
  de owner.
- `CUSTOMER` no puede usar endpoints administrativos.

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

## Standard error response

All error responses follow this JSON shape:

```json
{
  "timestamp": "2026-06-17T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "El horario seleccionado no está disponible",
  "path": "/api/v1/appointments",
  "code": "APPOINTMENT_NOT_AVAILABLE"
}
```

- `timestamp`: UTC instant of the error.
- `status`: HTTP status code.
- `error`: HTTP reason phrase.
- `message`: human readable message.
- `path`: request path.
- `code`: application specific error code (derived from exception name).


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
POST   /api/v1/appointments/walk-ins
GET    /api/v1/appointments/{id}
GET    /api/v1/barbers/{barberId}/appointments?date=2026-06-17
PATCH  /api/v1/appointments/{id}/cancel
PATCH  /api/v1/appointments/{id}/start
PATCH  /api/v1/appointments/{id}/complete
PATCH  /api/v1/appointments/{id}/no-show
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

Request `POST /api/v1/appointments/walk-ins`:

```json
{
  "customerId": 1,
  "barberId": 1,
  "serviceOfferingId": 1,
  "startAt": "2026-06-18T10:00:00",
  "startImmediately": true
}
```

Las citas walk-in se crean con `source = WALK_IN`. Si `startImmediately` es
`true`, la cita queda en `IN_PROGRESS`; si es `false`, queda en `SCHEDULED`.
Aplican las mismas reglas de cliente, barbero y servicio activos, horario
laboral activo y no solape con citas bloqueantes. Los recursos inactivos, el
solape y las citas fuera de horario responden `409 Conflict`.

Transiciones de estado:

```text
SCHEDULED -> IN_PROGRESS
IN_PROGRESS -> COMPLETED
SCHEDULED -> NO_SHOW
SCHEDULED -> CANCELLED
```

Las transiciones invalidas responden `409 Conflict`. Cada cambio de estado
actualiza `updatedAt` y no elimina fisicamente la cita.

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
