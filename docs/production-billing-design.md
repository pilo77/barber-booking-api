# Producción y suscripción básica

Fecha: 2026-10-08. Alcance autorizado: revisión del backend y frontend, acceso por
roles, reservas reales, alta de empresas, plan mensual y preparación/despliegue gratuito.
Se conserva el commit frontend e848ca5 sin reescribir historial.

## Decisiones

- Se mantiene la arquitectura hexagonal pragmática. Domain contiene modelos puros;
  application coordina transacciones mediante puertos; infrastructure implementa HTTP,
  seguridad, JDBC/JPA y pasarela. No se modifica ninguna migración existente.
- El plan BASIC cuesta 4000000 centavos COP (40000 pesos) por mes calendario y empresa.
  Una renovación añade un mes desde el vencimiento vigente o desde el pago si ya venció.
  El cliente renueva por transferencia con aprobación manual; no hay débito recurrente automático.
  El precio se sustenta como hipótesis de lanzamiento en [el estudio comercial](pricing-study-2026-10-08.md).
- La opción elegida por el usuario es transferencia Bre-B a Nequi. Los datos de destino
  se configuran mediante BILLING_MANUAL_INSTRUCTIONS; no se suben al repositorio.
  El propietario reporta la referencia; no obtiene acceso hasta que PLATFORM_OWNER
  verifica el ingreso y registra identificador bancario e importe coincidente con la orden.
  Aprobación, auditoría y renovación son transaccionales; la misma transacción no se reutiliza.
- La pasarela opcional es Wompi Checkout. No se habilita dinero real sin cuenta
  comercial verificada y aceptación de la comisión. Implementación deshabilitada por defecto.
- Monto, moneda, referencia, empresa y ambiente se fijan en el servidor. La idempotencia
  del checkout y del reporte manual se delimita por empresa. Si se habilita la pasarela,
  el pago se confirma por webhook autenticado y consulta backend a la transacción oficial.
  Una redirección o una captura no constituyen evidencia suficiente de pago.
- Un evento de sandbox nunca concede acceso en un despliegue de producción.
- Un pago aprobado se aplica una sola vez bajo locks en PostgreSQL. Se conservan
  estado de pedido, referencia de proveedor y fecha de confirmación para conciliación.
- La suscripción controla acceso operativo y booking público. Auth y billing permanecen
  disponibles al propietario para renovar. No se confunde pago de la plataforma con señas de citas.
- Alta de barbería crea company, branch, propietario y suscripción pendiente en una transacción.
  No concede roles de plataforma ni permite seleccionar ids de otro tenant.
- El frontend operativo tendrá componentes conectados a API, sin fallback a datos demo.
  Los componentes previos se conservan en archivos para evitar pérdida de trabajo.
- Sin infraestructura confirmada no se inventa una URL productiva ni se modifica
  environment.prod.ts. Se añade configuración cloud explícita para el despliegue real.

## Límite económico

Un presupuesto inicial de 20000 COP no compra dominio y hosting garantizados de por vida.
La propuesta inicial es Render Free + Neon Free y subdominios del proveedor, sujeto a
cuotas y cambios de condiciones. No usar PostgreSQL gratuito de Render como almacenamiento
permanente: caduca a los 30 días. La cuenta Render tiene plan gratuito; el proyecto Neon
barberia-ghs fue creado. El despliegue y los flujos en la URL real siguen pendientes de validación.

Fuentes oficiales consultadas: https://render.com/docs/free,
https://neon.com/pricing, https://wompi.com/es/co/planes-tarifas/,
https://docs.wompi.co/docs/colombia/widget-checkout-web/,
https://docs.wompi.co/docs/colombia/eventos/.

## Validación requerida

Maven verify; contratos REST; matriz de nueve roles; ownership y aislamiento;
Flyway limpio e incremental; restricciones y concurrencia PostgreSQL; duplicación,
firma alterada, importe/moneda incorrectos, sandbox/prod, pagos rechazados y renovación;
build y tests frontend; navegación pública y privada con API real; health y flujo crítico
en la URL desplegada. No declarar producción lista con comprobaciones pendientes.
