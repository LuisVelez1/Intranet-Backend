# Integración de Ausentismo y notificaciones a Talento Humano

## Versiones y alcance

Integración desde main `fedfd735812a0cc3ad771c5b3dbdb0e2b178af94` con
`feat/ausentismo` en `62fde2445bddb96e6c2190ddbf2364910c6ffe21`.
El desarrollo se realizó en un worktree local separado de producción.

Se conserva la versión avanzada: doble aprobación, rechazo inmediato del jefe,
aprobación/rechazo final de HR, carga y descarga segura de soportes, almacenamiento
persistente, tipos de ausencia, asignaciones, roles, permisos y consultas.
Los controladores, DTO, entidades, repositorios y almacenamiento de soportes son
los de `62fde24`; la modificación funcional adicional está en el encolado de correo.
No se modifica Requerimientos ni frontend.

## Notificaciones

| Evento | Destinatario principal | Entradas si principal no es HR |
| --- | --- | --- |
| Creación | Jefe asignado | 2: principal y copia HR |
| Rechazo del jefe | Colaborador | 2: principal y copia HR |
| Aprobación del jefe / remisión | HR | 1: remisión original |
| Aprobación final HR | Colaborador | 2: principal y copia HR |
| Rechazo final HR | Colaborador | 2: principal y copia HR |

Cada copia tiene exactamente el asunto y cuerpo del original. Se compara el
correo principal sin distinguir mayúsculas ni espacios exteriores. Si coincide
con HR, solo se crea una entrada. La notificación separada de creación que existía
en la versión avanzada se sustituye por la copia idéntica, evitando un tercer correo.
La remisión del jefe conserva su asunto y contenido propios y no agrega copia.

`ABSENCE_MAIL_HR_RECIPIENT` configura el buzón; su predeterminado es
`talentohumano@licodistribuciones.com`. Un valor vacío también usa el predeterminado.
El dispatcher, repositorio y entidad de `absence_mail_outbox` se conservan.
Cada entrada mantiene sus propios intentos y resultado. Ambas entradas se encolan
en la transacción de la solicitud; no se cambian correos previamente encolados.
La configuración SMTP y la condición `absence.mail.enabled=true` permanecen.

## Conflictos resueltos

- `AbsenceServiceImpl.java`: conservar la implementación avanzada y centralizar la
  copia en `enqueueEmail`, en lugar de duplicar la notificación HR de creación.
- `application.yaml`: conservar tanto `absence.support.dir` como `absence.mail.hr-recipient`.
- Tres archivos de pruebas: conservar las verificaciones de doble aprobación y
  soportes, actualizar conteos y conservar las pruebas de reintentos del PR #9.

## Compatibilidad con producción

El 11 de octubre de 2026 se consultaron exclusivamente metadatos de MySQL 8 en
`intranet_corporativa`, sin migraciones ni cambios de datos:

- `absence_requests` tiene las seis columnas de decisiones de jefe/HR, fechas,
  comentarios y `support_file`; tipos y nulabilidad corresponden a la versión avanzada.
- `ck_absence_status` admite `PENDING_HR`; las restricciones de decisiones permiten
  `APPROVED` y `REJECTED`, o NULL.
- `absence_types` existe con sus columnas, índice de actividad y unicidad del nombre.
- `absence_mail_outbox` existe con sus columnas y el índice de pendientes.
- Existe el rol `TALENTO_HUMANO` y una asignación de ese rol.

Las entidades y scripts SQL avanzados se incorporan sin cambios respecto a `62fde24`.
Este servidor ya tiene las estructuras necesarias: no se requiere una migración
adicional para la integración. Los scripts heredados, especialmente
`absence-double-approval.sql`, no son idempotentes: NO deben ejecutarse de nuevo
sobre esta base. Otros entornos que partan del main anterior requieren revisión
individual del esquema antes de adoptar la versión avanzada.

## Producción y siguiente paso

Diagnóstico previo: Compose efectivo `/opt/intranet/docker-compose.yml`, servicio
`backend`, contenedor `intranet-backend`, contexto `/opt/intranet/Backend-Intranet`.
SMTP está habilitado; el contenedor no define `ABSENCE_MAIL_HR_RECIPIENT`.
El código integrado usará el predeterminado. Una sobrescritura futura debe inyectarse
al backend mediante `environment`; definirla únicamente en `.env` no basta.

No se modifica Compose ni el entorno del servidor en este PR. No se realizan
reinicios ni despliegues. Tras revisión, merge y autorización expresa, preparar
respaldo restringido de configuración, conservar la imagen anterior, actualizar
el código al commit aprobado y construir/recrear exclusivamente `backend` con
`--no-deps`. Verificar salud y envío SMTP controlado antes de darlo por concluido.

Las pruebas automatizadas usan H2 y SMTP simulado, sin mensajes reales ni datos
productivos. La entrega SMTP real queda pendiente de una prueba controlada autorizada.

## Resultado de pruebas

`./mvnw test`: 100 pruebas, cero fallos, cero errores y cero omitidas.
Incluye los cinco eventos con y sin coincidencia del destinatario principal con HR,
configuración/valor vacío, controles de doble aprobación, carga/descarga y acceso a
soportes, rollback conjunto, decisiones concurrentes y fallas/reintentos de ambos
buzones. `git diff --check` pasó.
