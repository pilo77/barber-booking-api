# SaaS Roadmap for Barber Booking API

## Goal

Convertir `barber-booking-api` en una plataforma SaaS multi-barberia para
gestionar reservas, agenda, operacion diaria, pagos, caja, inventario y reportes
por empresa y sucursal.

## Target model

- Platform
- Company / BarberShop
- Branch
- Users
- Barbers
- Customers
- Services
- WorkingHours
- Appointments
- CashRegister
- Payments
- Receipts
- Products
- Inventory
- Reports
- Commissions

## Proposed roles

- `PLATFORM_OWNER`
- `COMPANY_OWNER`
- `BRANCH_MANAGER`
- `RECEPTIONIST`
- `BARBER`
- `CASHIER`
- `ACCOUNTANT`
- `INVENTORY_MANAGER`
- `CUSTOMER`

## Role responsibilities

| Role | Responsibilities |
| --- | --- |
| `PLATFORM_OWNER` | Administra la plataforma SaaS, planes, tenants, soporte y configuracion global. No opera citas o caja de una barberia salvo soporte controlado y auditado. |
| `COMPANY_OWNER` | Administra una empresa/barberia, sucursales, usuarios, roles, servicios, configuracion comercial y reportes globales de su tenant. |
| `BRANCH_MANAGER` | Opera una sucursal, gestiona barberos, horarios, agenda diaria, caja de la sucursal y reportes operativos. |
| `RECEPTIONIST` | Crea clientes, agenda citas, registra walk-ins, consulta disponibilidad y gestiona cambios de estado permitidos. |
| `BARBER` | Consulta su agenda, inicia/completa citas, ve su dashboard diario y consulta sus comisiones cuando aplique. |
| `CASHIER` | Abre/cierra caja, registra pagos, emite recibos, registra movimientos de efectivo y consulta cierres propios. |
| `ACCOUNTANT` | Consulta reportes financieros, cierres, pagos, recibos e impuestos. No modifica agenda ni inventario operativo. |
| `INVENTORY_MANAGER` | Administra productos, categorias, existencias, ajustes, restock, transferencias y devoluciones. |
| `CUSTOMER` | Consulta perfil publico, agenda citas, cancela dentro de reglas permitidas, ve historial y recibos propios. |

## Multi-tenant strategy

La plataforma debe proteger datos por `companyId` y, cuando aplique, por
`branchId`.

Principios:

- Todas las entidades comerciales nuevas deben tener `companyId`.
- Las entidades operativas de una sucursal deben tener `branchId`.
- Las citas, horarios, caja, inventario, pagos y reportes deben filtrar siempre
  por tenant.
- El tenant no debe confiarse desde el body del request cuando el usuario ya
  esta autenticado; debe derivarse del contexto de seguridad.
- Los usuarios internos pertenecen a una company y pueden tener permisos por
  branch.
- `PLATFORM_OWNER` puede operar a nivel plataforma, pero sus acciones deben
  quedar auditadas.
- Las restricciones de base de datos deben incluir tenant/branch en claves
  unicas relevantes, por ejemplo:
  - nombre de servicio unico por company/branch segun decision funcional;
  - email/telefono de customer unico por company si aplica;
  - horarios laborales por barber y branch;
  - cash register activo por branch/cashier.
- Los repositorios y puertos deben exponer metodos tenant-aware. Evitar metodos
  genericos que consulten datos sin `companyId`.
- Los tests deben cubrir que un tenant no pueda leer ni modificar datos de otro.

## Roadmap by user stories

### HU-17 Multi-tenant foundation

Agregar modelo base de `Company` y `Branch`, columnas `company_id` y
`branch_id` donde corresponda, estrategia de resolucion de tenant y primeras
restricciones de aislamiento.

Estado actual: implementada como foundation interna. Los endpoints existentes
siguen funcionando sin headers con tenant default `1/1`. Para pruebas de
aislamiento antes de HU-18 se puede enviar `X-Company-Id` y `X-Branch-Id`.
Estos headers son temporales; el cliente publico futuro debe resolver la
barberia por `slug`, no enviar ids de tenant en el body.

Alcance recomendado:

- Crear tablas `companies` y `branches`.
- Definir convencion de tenant en application layer.
- Migrar entidades actuales con estrategia backward compatible.
- Ajustar queries y puertos para filtrar por company/branch.
- Agregar tests de aislamiento.

### HU-18 Auth and RBAC

Agregar autenticacion y autorizacion por roles.

Estado actual: foundation implementada en rama de HU-18. Incluye bootstrap del
primer usuario, login JWT, `GET /auth/me`, usuarios internos minimos, roles y
tenant desde JWT para endpoints administrativos.

Alcance implementado:

- Modelo `UserAccount` y `Role`.
- Login seguro con JWT.
- Passwords con BCrypt.
- Guards por endpoint y caso de uso.
- Headers de tenant quedan solo como fallback si no hay usuario autenticado.
- No se imprimen tokens ni credenciales en logs.

Pendiente para futuras HUs:

- Relacion user-barber para limitar dashboard/agenda del rol `BARBER`.
- Auditoria persistente de acciones sensibles.
- Gestion completa multi-branch para `COMPANY_OWNER`.
- Roles de caja, contabilidad e inventario cuando existan esos modulos.

### HU-19 Barber public profile

Exponer perfil publico de barberia/sucursal/barbero para reserva online.

Alcance recomendado:

- Slug publico por company/branch/barber.
- Servicios disponibles.
- Horarios publicos.
- Reglas de visibilidad y datos no sensibles.

### HU-20 Online booking barber selection

Permitir que un cliente seleccione sucursal, servicio, barbero y slot
disponible.

Alcance recomendado:

- Disponibilidad tenant-aware.
- Slots por sucursal y barbero.
- Politicas contra doble reserva.
- Confirmacion de reserva online.

### HU-21 Cash register foundation

Crear la base de caja por sucursal.

Alcance recomendado:

- `CashRegister`.
- `CashRegisterSession`.
- Apertura con saldo inicial.
- Cierre con saldo contado.
- Movimientos de efectivo.
- Restriccion de caja activa por branch/cashier.

### HU-22 Payments and receipts

Registrar pagos y emitir recibos asociados a citas, walk-ins y ventas.

Alcance recomendado:

- Payment method.
- Mixed payments.
- Receipt numbering scoped by company/branch.
- Relacion con cash register cuando el metodo sea efectivo.
- Respuestas y errores estandarizados.

### HU-23 Products and inventory

Agregar productos, categorias e inventario.

Alcance recomendado:

- `Product`.
- `ProductCategory`.
- `InventoryItem`.
- `StockMovement`.
- Restock, sale decrement, adjustment, return and transfer.
- Auditoria por user/company/branch.
- Proteccion contra stock negativo.

### HU-24 Reports and commissions

Agregar reportes y comisiones.

Alcance recomendado:

- Ventas por rango de fecha.
- Citas por estado.
- Servicios por barbero.
- Comisiones por barbero.
- Cierres de caja.
- Movimientos de inventario.

### HU-25 Customer portal

Crear capacidades de autoservicio para clientes.

Alcance recomendado:

- Perfil de cliente.
- Historial de citas.
- Cancelacion bajo reglas.
- Recibos propios.
- Preferencias de comunicacion.

### HU-26 Ratings and reviews

Agregar calificaciones y reseñas despues de citas completadas.

Alcance recomendado:

- Rating por appointment completada.
- Comentario moderable.
- Promedio por barber/company.
- Proteccion contra reseñas duplicadas o de citas ajenas.

## Recommended implementation order

1. HU-17 Multi-tenant foundation.
2. HU-18 Auth and RBAC.
3. HU-19 Barber public profile.
4. HU-20 Online booking barber selection.
5. HU-21 Cash register foundation.
6. HU-22 Payments and receipts.
7. HU-23 Products and inventory.
8. HU-24 Reports and commissions.
9. HU-25 Customer portal.
10. HU-26 Ratings and reviews.

El orden recomendado empieza por multi-tenant porque company/branch afectan
todas las entidades y queries. Auth/RBAC debe venir despues para que el tenant
y los permisos salgan del contexto de seguridad y no del request body.

Los perfiles publicos y la seleccion de barbero vienen antes de caja porque
extienden el valor actual del MVP de reservas sin introducir contabilidad.
Caja, pagos y recibos deben implementarse antes de inventario para definir el
documento comercial que consumira stock. Inventario despues puede apoyarse en
pagos/recibos y registrar movimientos con trazabilidad. Reportes y comisiones
requieren datos confiables de agenda, pagos y caja. Portal de cliente y ratings
son capas de experiencia que conviene construir cuando el modelo operativo ya
este estable.

## Technical risks

- Refactor de entidades existentes para agregar `companyId` y `branchId`.
- Migraciones Flyway forward-only sobre tablas ya existentes.
- Backfill de datos actuales hacia una company/branch inicial.
- Filtrado incompleto por `companyId` o `branchId`.
- Endpoints antiguos que podrian exponer datos si quedan sin tenant filter.
- Seguridad y RBAC mal ubicados en controllers en vez de application policies.
- Backward compatibility con contratos actuales.
- Tests insuficientes para aislamiento entre tenants.
- Complejidad de caja, cierres y movimientos de efectivo.
- Concurrencia de citas, pagos e inventario.
- Reportes lentos si no se definen indices por tenant, branch y fecha.
- Tentacion de partir microservicios antes de estabilizar modulos y contratos.

## Final recommendation

La siguiente HU debe ser HU-17 Multi-tenant foundation.

No conviene implementar caja, pagos ni inventario antes de resolver tenant,
branch y estrategia de aislamiento. Esos modulos son financieros y operativos;
si nacen sin `companyId` y `branchId`, el refactor posterior sera mas riesgoso
y podria comprometer seguridad de datos entre barberias.
