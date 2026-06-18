# Architecture

## Objetivo

Construir una API de reservas para barberia con bajo acoplamiento, reglas de
negocio testeables y separacion clara entre dominio, aplicacion e
infraestructura.

## Capas

### Domain

Paquete: `com.villamil.barberbooking.domain`

Contiene modelos, value objects y excepciones de negocio. No debe importar
Spring, JPA, HTTP, JSON ni clases de infraestructura.

### Application

Paquete: `com.villamil.barberbooking.application`

Contiene casos de uso, comandos, respuestas y puertos.

- `port.in`: capacidades que ofrece el sistema.
- `port.out`: dependencias que la aplicacion necesita del exterior.
- `service`: implementaciones de casos de uso.
- `dto`: comandos y respuestas.

### Infrastructure

Paquete: `com.villamil.barberbooking.infrastructure`

Contiene adaptadores web, persistencia, configuracion e integraciones externas.
Aqui si pueden vivir Spring MVC, Spring Data JPA, PostgreSQL, Flyway y OpenAPI.

## Reglas de arquitectura

- Los controladores solo traducen HTTP a comandos y respuestas.
- La logica de negocio vive en dominio o servicios de aplicacion.
- Los servicios de aplicacion dependen de puertos, no de JPA.
- Las entidades JPA no se exponen directamente como respuestas HTTP.
- Las migraciones de esquema se versionan con Flyway.
- La base de datos puede reforzar consistencia, pero no reemplaza las reglas de
  negocio del caso de uso.

## Modelo inicial

- `Customer`: cliente de la barberia.
- `Barber`: barbero que atiende citas.
- `ServiceOffering`: servicio ofrecido, con duracion, precio y activacion logica.
- `BarberWorkingHour`: horario laboral recurrente por dia y barbero.
- `Appointment`: reserva o atencion walk-in.

## Regla anti doble reserva

La aplicacion debe validar cruces antes de guardar una cita. La base de datos
tambien refuerza la regla con una restriccion de exclusion sobre citas activas.

Una cita se cruza con otra cuando:

```text
existing.startAt < new.endAt AND existing.endAt > new.startAt
```

## Arquitectura Hexagonal (Portos y Adaptadores)

El proyecto sigue el estilo de arquitectura hexagonal para mantener bajo
acoplamiento y permitir pruebas aisladas del dominio. Las capas principales son:

- `domain`: modelos, value objects, reglas de negocio y excepciones. Sin
  dependencias de infraestructura (Spring, JPA, HTTP, etc.).
- `application`: casos de uso, DTOs, servicios de aplicacion y puertos.
  - `port.in`: puertos de entrada que describen las capacidades del sistema.
  - `port.out`: puertos de salida que describen dependencias externas (repos,
    integraciones).
- `infrastructure`: adaptadores que implementan puertos, configuracion, REST,
  persistencia e integraciones externas.

Los adaptadores traducen entre el mundo externo (HTTP, JPA, eventos) y los
puertos del dominio. Esto permite reemplazar implementaciones (por ejemplo,
persistencia o clientes HTTP) sin afectar la logica de negocio.

### Puertos de entrada y salida

- Puertos de entrada (`port.in`) son interfaces que exponen casos de uso a los
  adaptadores web o de otro tipo.
- Puertos de salida (`port.out`) son interfaces que la aplicacion necesita para
  persistir o consultar datos.
- Los adaptadores implementan estos puertos en la capa `infrastructure`.

### Adaptadores

- Adaptadores de entrada: controladores REST que traducen solicitudes HTTP a
  comandos de aplicacion.
- Adaptadores de salida: repositorios JPA, clientes externos y otros.

## Políticas y componentes importantes

- `AppointmentBookingPolicy`: componente de aplicacion que centraliza las
  decisiones de reservacion para evitar duplicacion de reglas entre citas
  `ONLINE` y `WALK_IN`. Esta politica valida existencia/actividad de recursos,
  cruces y las reglas de transicion de estado.
- `GlobalExceptionHandler`: manejador en la capa web que estandariza las
  respuestas de error (timestamp, status, error, message, path, code) y mapea
  excepciones de aplicacion a códigos y estados HTTP consistentes.
- OpenAPI/Swagger: la configuracion de Swagger se encuentra en la capa
  `infrastructure` y expone la UI y el JSON de especificacion para consumidores
  e integracion continua.

Estas decisiones permiten que la logica de negocio sea testeable y que la API
sea consistente para clientes y frontend.
