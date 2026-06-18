# RematePOS Reuse Analysis

## Context

`barber-booking-api` tiene `v0.9.0` como MVP estable. La API actual cubre
clientes, barberos, servicios, horarios laborales, citas, disponibilidad,
dashboard diario, ciclo de vida de citas, atenciones walk-in, Swagger/OpenAPI y
manejo estandar de errores.

Esta historia de usuario es solo de analisis y documentacion. No se modifico
codigo Java, no se tocaron migraciones Flyway y no se cambio logica de negocio.

## RematePOS location

RematePOS fue encontrado en:

```text
C:\Users\carlo\OneDrive\Escritorio\RematePos-Backend
```

Rutas revisadas:

- `C:\Users\carlo\OneDrive\Documents`
- `C:\Users\carlo\OneDrive\Escritorio`
- `C:\Users\carlo\Documents`
- `C:\Users\carlo\Desktop`

## Stack detected

- Lenguaje: Java 21.
- Framework: Spring Boot.
- Arquitectura de runtime: microservicios.
- Configuracion distribuida: Spring Cloud Config Server.
- Descubrimiento de servicios: Netflix Eureka Server y Eureka Client.
- API: REST con Spring Web MVC.
- Persistencia:
  - Product microservice: Spring Data JPA con PostgreSQL.
  - Customer microservice: Spring Data MongoDB.
- Validacion: Jakarta Validation en DTOs.
- Mapeo: mappers manuales con beans Spring.
- Errores: modulo comun `common-exceptions` con `GlobalExceptionHandler`.
- Contenedores locales: Docker Compose con PostgreSQL y MongoDB.
- Documentacion del README: menciona Swagger/OpenAPI, Spring Security,
  roles, ventas, facturacion electronica, inventario y escalabilidad
  multisucursal. En el codigo local revisado solo se observaron customer,
  product, category, stock purchase/restock, config server, discovery server y
  manejo comun de excepciones.

## Architecture detected

El proyecto localizado es un backend distribuido organizado como multi-module
Maven:

- `config-server`
- `discovery-server`
- `microservices`
  - `common-exceptions`
  - `customer-microservice`
  - `product-microservice`

La estructura interna de cada microservicio es principalmente por modulo y capa
tecnica:

- `controller`
- `service`
- `service.impl`
- `repository`
- `model`
- `dto`
- `mapper`
- `exceptions`

No es arquitectura hexagonal estricta. Los modelos de negocio del product
microservice son entidades JPA y viven cerca de repositorios Spring Data. El
customer microservice usa documentos MongoDB. La separacion es limpia para un
CRUD por capas, pero no mantiene dominio independiente de Spring/JPA/MongoDB
como requiere `barber-booking-api`.

## Useful modules

| Module | Evidence found | Usefulness for barber-booking-api SaaS |
| --- | --- | --- |
| `product-microservice` | Product CRUD, category relation, stock, purchase and restock endpoints | Base conceptual para HU-23 Products and inventory |
| `category` | Category CRUD and product grouping | Base conceptual para `PRODUCT_CATEGORY` |
| `customer-microservice` | Customer CRUD with address and city fields | Parcialmente reutilizable como referencia de datos comerciales |
| `common-exceptions` | Shared validation and generic error handling | Referencia conceptual; current API already has a stronger standard error shape |
| `config-server` | Centralized configuration service | Useful later only if the SaaS grows into multiple services |
| `discovery-server` | Eureka service discovery | Not needed for the current monolith; possible future cloud pattern |
| Docker Compose | Local PostgreSQL and MongoDB services | Useful as reference, but current API should keep its PostgreSQL/Testcontainers setup |
| README business scope | Inventory, sales, electronic invoices, roles, branches | Useful as roadmap input, not as implemented source code |

## Reuse strategy

### Can reuse conceptually

- Product catalog: name, description, price, image, category and stock.
- Product categories as a separate concept from barber services.
- Inventory adjustments through explicit operations instead of raw stock edits.
- Stock validation before a purchase/sale operation.
- Restock operation as an auditable inventory movement concept.
- Customer commercial profile fields such as address and city, if the SaaS
  later needs receipts, invoices or delivery-related data.
- Shared error-handling idea, while preserving the existing standard error
  response in `barber-booking-api`.
- Config/discovery patterns as future references if the platform becomes
  distributed.

### Can adapt with changes

- `purchaseProduct` should become an inventory reservation or stock decrement
  use case tied to a payment/receipt, not a controller-level stock command.
- `restockProduct` should become an inventory movement with reason, actor,
  branch, timestamp and source document.
- Product/category DTOs can inspire request/response shapes, but must be
  rewritten into `application.dto.command` and `application.dto.response`.
- Customer address/city can be added later through a forward-only migration and
  domain/application changes.
- Category/Product persistence should be adapted to PostgreSQL/Flyway and
  hexagonal ports, not copied as JPA-first domain.
- Config server/discovery should be deferred until there is operational need
  for multiple deployable services.

### Should not be copied directly

- JPA entities as business models.
- MongoDB customer persistence, because the current API is PostgreSQL/Flyway.
- Hard deletes for products, categories or customers.
- Controller method shapes that accept update DTOs without path IDs.
- Stock operations without branch, company, audit trail or concurrency strategy.
- Generic exception handling that hides all exceptions under a single internal
  server error shape.
- Microservice split at this stage. The current project should evolve first as
  a well-modularized monolith with hexagonal boundaries.
- Any local configuration values, credentials or environment-specific settings.

## Role equivalence

| RematePOS role/module | Barber Booking API equivalent |
| --- | --- |
| Owner/Admin | `COMPANY_OWNER` |
| Cashier | `CASHIER` |
| Customer | `CUSTOMER` |
| Product | `PRODUCT` |
| Category | `PRODUCT_CATEGORY` |
| Sale | `PAYMENT` / `RECEIPT` |
| Invoice | `RECEIPT` / `INVOICE` |
| Cash register | `CASH_REGISTER` |
| Branch/store | `BRANCH` |
| Inventory | `INVENTORY` / `STOCK_MOVEMENT` |
| Restock | `INVENTORY_ADJUSTMENT` |
| Purchase product | `SALE_ITEM` / `STOCK_DECREMENT` |
| Product microservice | `Product` bounded context/module |
| Customer microservice | Existing `Customer` module with SaaS tenant scope |
| Config server | Future platform configuration capability |
| Discovery server | Future service discovery capability |

## Business rules to reuse

### Cash register

No implemented cash-register module was found in the local RematePOS code, but
the POS domain suggests useful rules for the SaaS roadmap:

- A branch should have one active cash register session per cashier or per
  terminal, depending on the operational model.
- A cash register must be opened before recording cash payments.
- A cash register closure must reconcile expected cash vs counted cash.
- Opening balance, cash in, cash out, payments and closing balance must be
  auditable.

### Sales and payments

No implemented sales/payment module was found. Recommended rules to adapt:

- A payment must be associated with an appointment, walk-in service, product
  sale or manual branch charge.
- Payment methods should be explicit: cash, card, transfer, digital wallet or
  mixed payment.
- Cash payments should affect the active cash register.
- Payment creation should be idempotent from the API perspective when connected
  to external payment providers.

### Reports

No implemented report module was found. Useful report concepts:

- Daily branch sales.
- Barber commissions by service and date range.
- Cash register closure report.
- Inventory movement report.
- Product sales ranking.
- Appointment conversion and no-show report.

### Inventory

The product microservice implements stock decrement for purchase and stock
increment for restock. Reusable rules:

- Product stock starts at zero when omitted.
- Stock cannot be decremented below available quantity.
- Negative restock/purchase quantities are invalid.
- Product category must exist before creating or updating a product.
- Product update should not accidentally overwrite stock.

Required SaaS additions:

- Scope stock by `companyId` and `branchId`.
- Track every stock change as an immutable movement.
- Add movement type: sale, restock, adjustment, return, transfer.
- Add actor/user ID and business reason.
- Add optimistic or pessimistic locking for concurrent sales.

### Users and roles

The README mentions users and roles, but no implemented auth/RBAC module was
found in the local code. The useful concept is role separation for POS
operations:

- Owner/Admin manages company setup, branches and users.
- Cashier manages payments, receipts and cash register sessions.
- Inventory manager manages products, categories and stock movements.
- Accountant reads financial and closure reports.

## Risks

- RematePOS is microservice-oriented, while `barber-booking-api` is a
  hexagonal Spring Boot API. Copying structure directly would weaken current
  boundaries.
- Product and category models are JPA entities; copying them would introduce
  persistence concerns into domain.
- Customer persistence uses MongoDB; the target project uses PostgreSQL and
  Flyway.
- Some README capabilities are roadmap-level and were not found as implemented
  modules.
- Current RematePOS stock operations do not include tenant, branch, audit,
  receipt/payment linkage or concurrency safeguards.
- Hard deletes conflict with the existing soft-delete style for commercial
  records.
- Splitting into microservices before multi-tenant boundaries and auth are
  stable would increase deployment and operational complexity.

## Hexagonal migration recommendations

### domain

- Create pure domain models for `Product`, `ProductCategory`,
  `InventoryItem`, `StockMovement`, `CashRegister`, `CashRegisterSession`,
  `Payment` and `Receipt`.
- Keep domain free of Spring, JPA, HTTP and database annotations.
- Encode core invariants in domain/application policies:
  - no negative stock movements without explicit adjustment type;
  - no sale stock decrement below available stock;
  - no cash payment without an open cash register session;
  - no cross-company or cross-branch access.

### application

- Add use cases before persistence details:
  - create/list/update product;
  - create/list product category;
  - open cash register;
  - close cash register;
  - record cash movement;
  - record payment;
  - issue receipt;
  - register stock movement.
- Define ports for repositories and external payment/invoice providers.
- Use commands and responses instead of exposing persistence entities.
- Apply tenant context validation in every use case that touches scoped data.

### infrastructure

- Implement adapters with JPA entities, Spring Data repositories and mappers.
- Add forward-only Flyway migrations for each new table.
- Keep controllers thin: map HTTP requests to application commands only.
- Preserve current `GlobalExceptionHandler` response format.
- Add database constraints for tenant/branch foreign keys and stock movement
  consistency.
- Add indexes by `company_id`, `branch_id`, date and status for SaaS queries.

## Final recommendation

Reuse RematePOS first as conceptual input for products, categories and
inventory movements. Do not copy code directly.

The next implementation should not start with products or cash register yet.
The safest next HU is multi-tenant foundation, because every later POS concept
must be scoped by company and branch from day one.
