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

## Services

```http
POST   /api/v1/services
GET    /api/v1/services
GET    /api/v1/services/{id}
PUT    /api/v1/services/{id}
DELETE /api/v1/services/{id}
```

## Appointments

```http
POST   /api/v1/appointments
GET    /api/v1/appointments/{id}
GET    /api/v1/barbers/{barberId}/appointments?date=2026-06-17
PATCH  /api/v1/appointments/{id}/cancel
PATCH  /api/v1/appointments/{id}/start
PATCH  /api/v1/appointments/{id}/complete
```

## Availability

```http
GET /api/v1/barbers/{barberId}/availability?date=2026-06-17&serviceId=1
```

Respuesta esperada:

```json
{
  "barberId": 1,
  "date": "2026-06-17",
  "slots": [
    {
      "startTime": "08:00",
      "endTime": "08:30",
      "status": "AVAILABLE",
      "color": "GREEN"
    }
  ]
}
```
