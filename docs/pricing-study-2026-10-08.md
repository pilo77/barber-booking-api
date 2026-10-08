# Estudio de precio — Barbería GHS

Fecha de consulta: 8 de octubre de 2026. Moneda del plan: pesos colombianos (COP).

## Recomendación y alcance

Precio de lanzamiento: **$40.000 COP por mes calendario y barbería**. Es una hipótesis
comercial razonable para una barbería pequeña que necesita agenda, reservas web y
administración de equipo. Se sustituye el precio inicial de $5.000 por instrucción del
propietario. La configuración del servidor usa **4.000.000 centavos COP**.

No es una valoración de venta del código fuente ni una garantía de rentabilidad.
El estudio combina revisión del código local, precios publicados y escenarios de
costos explícitos. No se han hecho entrevistas comerciales ni pruebas de disposición
a pagar. La aplicación aún no está validada en un despliegue de producción.

Mantener un único plan al lanzamiento. Revisar el precio después de dos ciclos de
cobro con clientes reales; considerar $49.900 para nuevas altas si los costos,
retención y calidad del servicio lo justifican. No aumentar automáticamente a clientes
existentes ni anunciar funciones pendientes como incluidas.

## Qué puede respaldar el precio

| Función | Evidencia en el proyecto | Situación comercial |
| --- | --- | --- |
| Alta de barbería y propietario | CompanyOnboardingService; formulario SaaS | Implementada; falta validar el flujo completo en nube |
| Agenda por barbero y disponibilidad | Casos de uso, endpoints y pantalla de operaciones | Implementada; requiere validación visual y productiva |
| Reserva pública por barbería/sede | PublicBookingService; frontend public-booking | API real, idempotencia y controles de disponibilidad; falta validación cloud |
| Clientes, servicios, equipo y horarios | Servicios administrativos y frontend operations | Implementados para roles autorizados |
| Iniciar, completar, cancelar y marcar inasistencia | Casos de uso de appointments | Implementados; no equivalen a facturar ni cobrar el servicio del barbero |
| Separación entre barberías y acceso por roles | JWT, políticas y pruebas de seguridad | Nueve roles existentes; caja, contabilidad e inventario tienen acceso restringido, sin módulos completos |
| Cobro de la suscripción | ManualBillingService, órdenes y auditoría de revisión | Transferencia reportada; solo el administrador de plataforma aprueba tras comprobar el ingreso |
| Pasarela automática | Adaptador Wompi y verificación de eventos | Preparada y deshabilitada; no hay cuenta comercial ni integración de dinero real validada |

No vender todavía como incluidos: caja/POS completo, inventario, comisiones, reportes
financieros, facturación electrónica, recordatorios automáticos por WhatsApp/SMS,
recuperación de contraseña, MFA ni débito mensual automático. Su existencia en una
hoja de ruta o en el nombre de un rol no demuestra que funcionen.

El modelo permite empresas y sedes, pero el plan actual no impone cuotas comerciales
de sedes, barberos o citas. El público inicial recomendado es una barbería pequeña.
Antes de ofrecer planes con límites específicos o acceso ilimitado, definirlos,
implementarlos y medir capacidad; no hay prueba de carga para prometer un volumen.

## Referencias de mercado

Precios anunciados por los propios proveedores; las condiciones y funciones no son
equivalentes. No se contactó a vendedores ni se verificó el total de un checkout.

| Proveedor / plan | Precio publicado | Diferencia relevante |
| --- | --- | --- |
| [AgendaPro Colombia — Individual](https://agendapro.com/co/planes-int) | Desde $29.900 COP/mes | Dirigido a independientes; anuncia CRM, recordatorios y reportes |
| [AgendaPro Colombia — Básico](https://agendapro.com/co/planes-int) | Desde $114.000 COP/mes por sucursal, según configuración | Añade inventario y comisiones; no es comparable directamente con una agenda básica |
| [NEXBIO — Agenda Individual](https://nexbio.pro/barberia) | $49.900 COP/mes | Anuncia 1–2 sillas, reservas web y avisos; bot de agenda fuera de este plan |
| [NEXBIO — Agenda Equipo](https://nexbio.pro/barberia) | $119.900 COP/mes | Anuncia hasta 3 sillas, caja y seña |
| [WeiBook — HomeStudio](https://weibook.co/es/plans) | US$15/mes | Un profesional; sitio personalizado y correos de citas |
| [WeiBook — Ultra](https://weibook.co/es/plans) | US$39/mes al contratar anual | Hasta 10 colaboradores; POS, caja y comisiones |

AgendaPro incluye precios cargados dinámicamente: los importes de planes fueron
observados en el resultado indexado de su página oficial durante esta consulta.
Se deben reconfirmar en el selector antes de una comparación comercial definitiva.
WeiBook publica en USD: no se convierte a COP sin una tasa y fecha verificadas.

Interpretación: $40.000 queda entre dos referencias de entrada en COP. GHS debe
competir por facilidad de uso, adopción y atención cercana; no por afirmar que tiene
todas las funciones de productos más maduros. Ser más barato no demuestra que el
cliente prefiera GHS.

## Costo de cobrar cada mensualidad

| Medio | Sobre $5.000 | Sobre $40.000 | Condiciones |
| --- | ---: | ---: | --- |
| Bre-B en Nequi, aprobación manual | $0 de comisión Nequi | $0 de comisión Nequi | El tiempo de revisar el ingreso sí cuesta; otros bancos definen sus tarifas |
| Wompi estándar: 2,65% + $700 + IVA | $990,68 incluyendo IVA del 19% sobre la comisión | $2.094,40 incluyendo ese IVA | Cálculo ilustrativo; no incluye retenciones ni otros efectos tributarios |
| Wompi QR: tarifa publicada 1% | $50 de comisión base | $400 de comisión base | QR específico, personas naturales y pagos desde Bancolombia/Nequi; reconfirmar impuestos y habilitación |
| Bold QR Online: desde 2,89% | $144,50 de comisión base | $1.156 de comisión base | Cuenta digital Bold y ventas hasta $10 millones/mes; página excluye IVA y retenciones |

Fuentes: [Nequi/Bre-B](https://ayuda.nequi.com.co/hc/es/articles/34000109372045--Qu%C3%A9-es-Bre-B-y-c%C3%B3mo-funciona-en-Colombia),
[tarifas Wompi](https://wompi.com/es/co/planes-tarifas/),
[QR en la API Wompi](https://docs.wompi.co/docs/colombia/metodos-de-pago/),
[Bold pagos en línea](https://www.bold.co/pagos-en-linea/pasarela-de-pagos?hide-splash=true).

El ejemplo usa la [tarifa general de IVA del 19% indicada por la DIAN](https://normograma.dian.gov.co/dian/compilacion/docs/oficio_dian_8857_2025.htm).
El IVA del ejemplo corresponde a la comisión de Wompi; no define el tratamiento
tributario de la suscripción GHS. No se ha determinado la situación fiscal del
propietario, por lo que no se calcula utilidad después de impuestos.

Se mantiene la decisión vigente del usuario: transferencia con aprobación manual.
Para automatizar, QR Wompi merece evaluación por su menor costo; hay que verificar
habilitación, destino de abonos y condiciones de la cuenta. Una llave Bre-B personal
por sí sola no da a GHS una confirmación automática autenticada del banco. Cada
mes el cliente realiza el pago; activación automática no significa débito automático.

## Modelo económico: supuestos, no facturas

Escenario de referencia para transferencias manuales:

- Precio: $40.000 por cliente que efectivamente paga cada mes.
- Soporte por cliente: 15 minutos/mes, valorando el trabajo a $20.000/hora: $5.000.
- Revisión de transferencia: 5 minutos/mes a la misma tarifa: $1.666,67.
- Reserva de contingencia: 10% del ingreso: $4.000 por cliente.
- Presupuesto fijo mensual de referencia: $200.000, compuesto por $100.000 para
  futura infraestructura/respaldos y $100.000 para cinco horas de mantenimiento común.
  Es una provisión ilustrativa; **no es una cotización cloud ni un gasto autorizado**.
- Contribución por cliente antes de costos fijos: $29.333,33.
- Punto de equilibrio de este escenario: 7 clientes pagadores.

| Clientes pagadores | Ingreso mensual | Soporte, revisión y reserva | Presupuesto fijo | Saldo estimado |
| ---: | ---: | ---: | ---: | ---: |
| 5 | $200.000 | $53.333 | $200.000 | -$53.333 |
| 10 | $400.000 | $106.667 | $200.000 | $93.333 |
| 20 | $800.000 | $213.333 | $200.000 | $386.667 |
| 50 | $2.000.000 | $533.333 | $200.000 | $1.266.667 |

Fórmula: saldo = clientes × precio − clientes × (soporte + revisión + reserva) − fijo.
Importes redondeados al peso al presentar. No se incluye adquisición de clientes,
comisiones de vendedores, implementación inicial, recuperación del desarrollo,
devoluciones, cartera, impuestos o incidentes extraordinarios. No confundir saldo
del escenario con utilidad neta ni tomar 50 clientes como capacidad técnica validada.

Sensibilidad: con una hora de soporte por cliente/mes, la contribución cae a
$14.333,33 y el equilibrio sube a 14 clientes. A $5.000, con los 20 minutos de trabajo
anteriores y una reserva del 10%, la contribución sería negativa incluso antes de
pagar infraestructura: por eso ese precio inicial no es sostenible bajo estos supuestos.

Ejemplo de valor para la barbería: si el margen real por una cita adicional fuera
$10.000, necesitaría cuatro citas adicionales al mes para recuperar $40.000. El
margen debe calcularlo cada negocio; la aplicación no garantiza nuevas citas.

## Nube y lanzamiento

El presupuesto solicitado para subir a la nube es hasta $20.000 COP de pago único.
Se mantiene la preparación en planes gratuitos, sin contratar pagos como resultado
de este estudio. Los ingresos de la suscripción no son una autorización de gasto.

[Render Free](https://render.com/docs/free) suspende el backend tras 15 minutos sin
tráfico y el primer acceso puede esperar aproximadamente un minuto. Sus cuotas y
las de [Neon](https://neon.com/pricing) limitan el servicio; no garantizan hosting,
dominio propio o permanencia de por vida. El piloto debe comunicar esta espera.
Para vender un servicio con disponibilidad continua hay que presupuestar operación
continua, monitoreo y recuperación; esos requisitos no quedan resueltos por cobrar $40.000.

No empezar cobros comerciales hasta comprobar en la URL real: login, aislamiento
de tenants, permisos, horarios, reserva, transiciones de cita, aprobación/rechazo de
transferencias y vencimiento del plan. También definir contacto de soporte,
tratamiento de datos, términos de pago y un procedimiento probado de respaldo/restauración.

## Validación del precio durante los primeros dos meses

1. Entrevistar al menos 5 barberías del segmento objetivo y presentar el mismo alcance.
2. Medir activación: configuración de servicios/horarios y primera reserva real.
3. Medir clientes que pagan $40.000, renuevan y cancelan, sin confundir registros con ventas.
4. Registrar minutos de soporte, conciliación, fallas, ocupación de base y tráfico cloud.
5. Calcular contribución con costos observados. Mantener $40.000 si funciona; ajustar
   alcance o precio para nuevas altas si soporte y operación consumen el margen.

## Aplicación del cambio en el código

- application.yml y los defaults de BillingService/ManualBillingService: 4.000.000 centavos.
- Landing SaaS: $40.000 COP/mes por barbería; pantalla Mi plan lee el precio del backend.
- No modificar migraciones ni recalcular órdenes existentes: conservan el importe
  registrado al crearse y ese importe determina la validación del pago.
- Una variable BILLING_BASIC_AMOUNT_IN_CENTS preexistente prevalece sobre el default:
  comprobarla sin mostrar secretos antes de desplegar. Si se cambia la tarifa por
  ambiente, mantener coherente la landing y reconstruir el frontend.
- Se conservan pruebas con órdenes antiguas de $5.000 para verificar compatibilidad;
  se añade prueba de nuevos pedidos a $40.000 y rechazo de aprobación por importe menor.

## Resultado inicial de la validación del cambio de precio

- Backend: rama feature/production-readiness-billing; mvnw.cmd verify: BUILD SUCCESS.
  351 pruebas reportadas, 341 ejecutadas sin fallos y 10 omitidas por indisponibilidad
  de Docker/Testcontainers. Las suites adicionales de facturación y seguridad sí
  ejecutaron 18 pruebas contra PostgreSQL local real, versión 18, en bases aisladas.
- Frontend: rama feature/production-readiness-ui; npm test -- --watch=false:
  9 pruebas aprobadas; npm run build: correcto.
- git diff --check: correcto en ambos repositorios.
- Endpoints: no se añaden ni se cambian rutas por el precio; subscription devuelve
  la nueva tarifa, y nuevos checkout/reportes de transferencia usan ese importe.
- Seguridad: el cliente no selecciona el valor del plan; aprobación de importe
  inferior rechazada; se conserva aprobación exclusivamente por PLATFORM_OWNER.
- Migraciones: ninguna creada o modificada por el cambio de precio. V15 y V16
  pertenecen al trabajo de suscripciones previamente preparado.
- Git: hay cambios locales de preparación de producción en ambos repositorios,
  incluidos los archivos de este estudio. No se creó un commit ni se hizo push,
  merge o despliegue por este cambio.
- Recomendación: revisión comercial y piloto controlado cuando se cierre la
  validación productiva. No mergear como entrega de producción completa todavía.

La revisión posterior amplió la validación a 408 pruebas backend reportadas localmente
(398 ejecutadas, 10 omitidas), 26 casos con PostgreSQL real y 21 pruebas frontend.
Las ramas se publicaron y la CI remota ejecutó las 408 pruebas backend sin omisiones,
con Docker/PostgreSQL 16, además de completar la validación del frontend.
El flujo manual y la reserva pública se comprobaron en navegador con datos ficticios.
El estado final y los bloqueos de nube constan en
[el informe de preparación](production-readiness-report.md); los resultados iniciales
anteriores se conservan como evidencia de la etapa del cambio de precio.
