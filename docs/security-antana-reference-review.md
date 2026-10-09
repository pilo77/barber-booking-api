# Revisión de la referencia Antana y controles adoptados en GHS

Fecha: 2026-10-08. Fuente proporcionada por el usuario: `antana original.zip`.

## Alcance y evidencia

Se inspeccionaron los archivos del ZIP sin ejecutar sus scripts, páginas, SQL ni funciones remotas. Es una aplicación HTML/JavaScript que utiliza Supabase Auth, PostgreSQL con RLS y Edge Functions. Esta revisión identifica patrones de código; no certifica la configuración ni la seguridad del despliegue de Antana.

Archivos revisados: `login.html`, `registro.html`, `recuperar.html`, `reset-password.html`, `perfil.html`, `supabase/schema.sql` y las funciones `create-delivery-driver/index.ts` y `update-delivery-driver/index.ts`. Los valores de credenciales y los identificadores personales se excluyeron del informe.

## Patrones que sirven

| Patrón de Antana | Aplicación en GHS |
| --- | --- |
| Autenticación validada por un servidor de identidad | Mantener Spring, BCrypt y JWT firmado. Cada petición autenticada vuelve a comprobar la cuenta y sus permisos en la base de datos. |
| Funciones administrativas verifican al llamador antes de usar credenciales privilegiadas | Mantener permisos explícitos por rol y tenant, denegar rutas desconocidas y no aceptar roles o tenant enviados por el cliente. |
| RLS restringe registros por identidad | Conservar consultas, claves foráneas compuestas y casos de uso tenant-aware. RLS no sustituye las reglas de negocio. |
| Confirmación de contraseña, estado de carga y mostrar/ocultar contraseña | Incorporados al formulario de seguridad de GHS; evita peticiones duplicadas y mantiene feedback accesible. |
| Recuperación termina cerrando la sesión | El cambio de contraseña de GHS incrementa una versión persistida y revoca los JWT anteriores, incluida cualquier otra pestaña o dispositivo. |
| Variables de entorno para credenciales privilegiadas | Mantener secretos fuera del repositorio y evitar errores/logs que los revelen. |
| Trigger `SECURITY DEFINER` con `search_path` vacío | Es una precaución adecuada cuando se requiere ese tipo de función; no se añade un trigger de identidad ajeno al modelo de GHS. |

## Aspectos que no deben copiarse

| Hallazgo de la referencia | Riesgo y decisión |
| --- | --- |
| Login utiliza `app_metadata.role` con fallback a `user_metadata.role` | El usuario puede editar `user_metadata`; no puede ser fuente de autorización. En GHS, roles y permisos proceden de la base de datos y del backend. |
| Identidad del administrador definida por un correo literal en políticas/funciones | No ofrece gestión clara del ciclo de vida del privilegio. GHS conserva `PLATFORM_OWNER` asignado de forma controlada. |
| Contraseñas nuevas con mínimo de seis caracteres | Mantener mínimo de doce caracteres y límite real de BCrypt de 72 bytes UTF-8 para nuevas contraseñas. No degradar la política existente. |
| Políticas de actualización de pedidos de repartidores verifican la asignación, sin limitar por sí mismas todas las columnas y transiciones | La propiedad de una fila no autoriza cualquier modificación. Mantener comandos específicos, transiciones de dominio y permisos por operación en GHS. |
| Inserción de pedidos/items permite campos comerciales suministrados por el cliente | Calcular precios, importes, estados, tenant y duración en el servidor. Esta inspección no verificó controles adicionales en la instancia real de Antana. |
| Algunas funciones devuelven errores internos de Auth/base de datos al cliente | Usar respuestas controladas, sin credenciales, SQL ni detalles técnicos innecesarios. |

La documentación oficial de [Supabase Users](https://supabase.com/docs/guides/auth/users) confirma que `user_metadata` es editable por el usuario y no debe utilizarse para autorización. Los patrones de [contraseñas y recuperación](https://supabase.com/docs/guides/auth/passwords) dependen del proveedor de identidad y del canal de correo.

## Cambio concreto de seguridad

`POST /api/v1/auth/change-password` requiere una sesión válida y `{oldPassword,newPassword}`. El backend verifica la contraseña actual, valida la nueva, actualiza el hash con comparación del valor anterior e incrementa la versión de sesión en la misma transacción. Los tokens anteriores dejan de autenticar; un nuevo login verifica las credenciales actualizadas antes de emitir un token.

La comparación del hash al emitir el JWT impide que un login que validó la contraseña antigua antes de un cambio termine emitiendo una sesión válida después del cambio. Dos cambios simultáneos no pueden sobrescribirse utilizando el mismo hash anterior. Se aplica limitación de peticiones sin leer ni registrar las contraseñas en el filtro.

El cliente muestra éxito y cierra su sesión únicamente después de la confirmación del servidor. Una respuesta tardía de otra sesión no borra un login más reciente. No se almacenan las contraseñas en storage ni se incluyen en las respuestas.

## Funciones que siguen requiriendo un canal real

Confirmación de correo, recuperación por enlace, Google OAuth y MFA son capacidades distintas. No se consideran operativas por tener un formulario o por utilizar una dirección Gmail.

Para recuperación/confirmación se necesita un emisor configurado, una URL HTTPS real, tokens aleatorios almacenados mediante hash, vencimiento, consumo único, límites por cuenta/IP y respuestas que no revelen si una cuenta existe. Deben probarse la entrega y las listas de redirección permitidas. [Supabase SMTP](https://supabase.com/docs/guides/auth/auth-smtp) tampoco garantiza correo de producción sin una configuración adecuada.

GHS conserva su arquitectura hexagonal; no migra su identidad a Supabase a partir de este ZIP. Esta entrega incorpora cambio autenticado y revocación; no promete correo enviado, recuperación por email ni autenticación Google sin integración verificada.
