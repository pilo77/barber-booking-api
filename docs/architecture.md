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

- `Company`: empresa/barberia tenant del sistema SaaS.
- `Branch`: sede de una `Company`; agrupa operacion fisica como barberos,
  horarios y citas.
- `Customer`: cliente de la barberia.
- `Barber`: barbero que atiende citas.
- `ServiceOffering`: servicio ofrecido, con duracion, precio y activacion logica.
- `BarberWorkingHour`: horario laboral recurrente por dia y barbero.
- `Appointment`: reserva o atencion walk-in.

## Multi-tenant foundation and Auth/RBAC

La base SaaS usa `companyId` y `branchId` para aislar datos entre barberias.
`Customer` y `ServiceOffering` quedan scopeados por `companyId`; `Barber`,
`BarberWorkingHour` y `Appointment` quedan scopeados por `companyId` y
`branchId`.

Desde HU-18, los endpoints administrativos resuelven el tenant desde el usuario
autenticado por JWT. El token contiene:

- `userId`
- `email`
- `companyId`
- `branchId`
- `roles`

La prioridad de resolucion es:

1. JWT valido en `Authorization: Bearer <token>`.
2. Headers temporales de desarrollo/testing si no hay usuario autenticado.
3. Tenant default `1/1`.

Los headers HTTP temporales son:

- `X-Company-Id`
- `X-Branch-Id`

Si existe JWT valido, los headers no pueden sobrescribir `companyId` ni
`branchId`. Los cuerpos de los requests no aceptan `companyId` ni `branchId`;
el tenant se obtiene desde el contexto resuelto por infraestructura. En HU-19
los endpoints publicos resuelven la barberia por `companySlug + branchSlug`, no
por ids enviados por el cliente. `TenantContextExecutor` instala ese contexto
solo durante el caso de uso publico y restaura el contexto anterior al salir.
El filtro temporal ignora headers de tenant en rutas `/api/v1/public/**`.

La seguridad vive en `infrastructure.config.SecurityConfig` y adapters de
`infrastructure.security`. El dominio mantiene `UserAccount` y `Role` sin
dependencias de Spring Security, JPA ni HTTP.

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

- `AppointmentBookingPolicy`: componente de aplicación que centraliza las
  decisiones de reservación para evitar duplicación de reglas entre citas
  `ONLINE` y `WALK_IN`. Esta política valida existencia/actividad de recursos,
  cruces y las reglas de transición de estado. Observación: actualmente esta
  clase está registrada como bean de Spring (p. ej. `@Component`) dentro de la
  capa `application` por practicidad. Esto facilita la inyección en los
  casos de uso pero introduce una dependencia a Spring en la capa de
  aplicación.

- `BarberAvailabilityCalculator`: componente compartido por disponibilidad
  administrativa y publica. Calcula slots desde horarios y citas bloqueantes;
  la autorizacion y la visibilidad publica se validan antes de invocarlo.

- `PublicBookingService`: resuelve tenant y visibilidad por slugs, ejecuta los
  repositorios tenant-aware dentro de un contexto acotado, reutiliza o crea el
  customer por telefono y delega horarios/solape a `AppointmentBookingPolicy`.

- `GlobalExceptionHandler`: manejador en la capa web que estandariza las
  respuestas de error (timestamp, status, error, message, path, code) y mapea
  excepciones de aplicación a códigos y estados HTTP consistentes.

- OpenAPI/Swagger: la configuración de Swagger se encuentra en la capa
  `infrastructure` y expone la UI y el JSON de especificación para consumidores
  e integración continua.

Estas decisiones permiten que la lógica de negocio sea testeable y que la API
sea consistente para clientes y frontend.

## Notas sobre pragmatismo arquitectónico

- Dominio puro: la capa `domain` se mantiene libre de dependencias de Spring,
  JPA y HTTP. Las entidades, value objects y excepciones de negocio no conocen
  el framework.

- Capa `application` pragmática: en esta versión se permite el uso de
  anotaciones de Spring (`@Service`, `@Component`) en los servicios de
  aplicación para simplificar la inyección de dependencias y la interoperación
  con adaptadores. Esta es una decisión intencional de pragmatismo que acelera
  desarrollo y pruebas locales.

- Alternativa estricta: para una implementación más fiel a la hexagonalidad se
  puede convertir las clases de `application` en POJOs y registrar los beans
  exclusivamente desde la configuración en `infrastructure` (por ejemplo,
  clases `@Configuration` que construyan y expongan los servicios). Esa
  refactorización está documentada como deuda técnica y puede abordarse cuando
  se requiera mayor separación de responsabilidades.

Al documentar esta decisión explícitamente evitamos confusiones en entrevistas
o auditorías y dejamos claro qué partes del sistema son "puramente" agnósticas
al framework y cuáles usan Spring por pragmatismo.
