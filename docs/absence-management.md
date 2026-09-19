# Ausentismo — Backend fase 1

Implementación por capas: controller → service/impl → repository → entidad JPA.
No modifica User, áreas, cargos, autenticación JWT ni funcionalidades existentes.

## Puesta en marcha

1. Revisar y ejecutar `docs/sql/absence-management.sql` sobre la base de despliegue antes de iniciar la aplicación. Hibernate permanece en `ddl-auto: validate`. El script no se ejecuta automáticamente ni contiene cambios a tablas existentes. Revisar que las columnas de referencia tengan el mismo charset/collation que `users.id`.
2. Configurar una fila en `absence_approver_assignments` por empleado. `employee_id` y `approver_id` referencian los UUID existentes de `users`, no sus nombres de usuario. Ambos usuarios deben estar activos y ser distintos. No se agregan endpoints administrativos en esta fase.
3. Para reasignar, actualizar la fila existente; para desactivar, establecer `active = 0`. La restricción única evita asignaciones ambiguas. Las solicitudes ya creadas conservan su aprobador original.
4. Configurar las variables de correo en el proceso Java y habilitar el envío. El archivo `.env` no se carga por sí solo en Spring Boot.

```text
ABSENCE_MAIL_ENABLED=true
ABSENCE_MAIL_HOST=<host SMTP>
ABSENCE_MAIL_PORT=587
ABSENCE_MAIL_USERNAME=<usuario SMTP>
ABSENCE_MAIL_PASSWORD=<secreto externo>
ABSENCE_MAIL_FROM=<remitente autorizado>
ABSENCE_MAIL_STARTTLS=true
ABSENCE_MAIL_AUTH=true
```

`absence.mail.poll-ms` controla el intervalo (5000 ms por defecto). Se puede indicar con `--absence.mail.poll-ms=5000`.
Cuando `ABSENCE_MAIL_ENABLED` no es `true`, las notificaciones quedan guardadas pendientes y no se intenta enviar correo. No se suministran credenciales ni destinatarios reales en el código. Al habilitarlo se procesan también los pendientes anteriores.

## Contrato HTTP

El prefijo `/api` procede del context-path existente.

| Método | Ruta | Resultado |
| --- | --- | --- |
| POST | `/api/absences` | 201 con la solicitud creada en PENDING |
| GET | `/api/absences/my` | Solicitudes del usuario autenticado, más recientes primero |
| GET | `/api/absences/pending` | Solicitudes pendientes asignadas al usuario autenticado |
| PUT | `/api/absences/{id}/approve` | Solicitud actualizada a APPROVED |
| PUT | `/api/absences/{id}/reject` | Solicitud actualizada a REJECTED |

Creación:

```json
{
  "type": "PERSONAL",
  "startDate": "2026-10-01",
  "endDate": "2026-10-01",
  "startTime": "09:15",
  "endTime": "10:30",
  "reason": "Diligencia personal"
}
```

Decisión: `{ "comment": "Comentario opcional" }`; enviar `{}` si no hay comentario.

El frontend no determina solicitante, aprobador, estado ni fechas de auditoría. El servicio consulta siempre `SecurityContext` y carga el usuario por username. Incluso un SUPER_ADMIN no puede decidir si no es el aprobador de esa solicitud.

DTO de respuesta: `id`, `requesterId`, `requesterName`, `approverId`, `approverName`, `type`, `startDate`, `endDate`, `startTime`, `endTime`, `reason`, `supportFile`, `status`, `createdAt`, `updatedAt`, `approvedAt`, `approvalComment`. No expone la entidad User ni contraseñas.

Reglas:

- `type`: obligatorio, hasta 100 caracteres; texto libre en esta fase.
- `reason`: obligatorio, hasta 2000 caracteres; comentario opcional hasta 2000.
- Fechas obligatorias. Sin horas, el rango representa días completos inclusivos; un solo día es válido.
- Indicar ambas horas o ninguna. Con horas, el fin debe ser estrictamente posterior al inicio. Se exige precisión de minutos.
- Se usa fecha/hora local del servidor para auditoría. No se convierten los periodos a UTC ni se calculan horas laborales.
- Solo PENDING puede cambiar a APPROVED o REJECTED. Las decisiones se serializan mediante bloqueo de fila dentro de una transacción.
- `approvedAt` solo se establece al aprobar. En rechazo se conserva `updatedAt` como fecha de modificación.
- `CANCELLED` está definido como estado terminal; no hay endpoint de cancelación en el alcance solicitado.
- `supportFile` es nullable; no se implementa carga de adjuntos en esta fase.
- Errores: 400 datos inválidos/sin aprobador; 403 usuario inactivo o decisión ajena; 404 solicitud inexistente; 409 estado ya decidido o conflicto de bloqueo. Se mantiene el comportamiento JWT existente para accesos anónimos.

## Correos y recuperación

La solicitud/decisión y el correo pendiente se guardan en una misma transacción. El dispatcher solo ve filas confirmadas. El envío se realiza en segundo plano cada cinco segundos cuando está habilitado.

El aprobador recibe la creación; el empleado recibe aprobación o rechazo. Los mensajes incluyen identificador y periodo, sin copiar motivos ni comentarios potencialmente sensibles. Se consulta el detalle dentro de la intranet autenticada; no hay aprobación por enlace de correo.

Los fallos SMTP mantienen la notificación pendiente y aumentan el intervalo de reintento entre 1 y 60 minutos. `attempts`, `next_attempt_at`, `last_error` y `sent_at` permiten revisar el estado. No se guarda el mensaje técnico de error ni credenciales.

La entrega es al menos una vez: si SMTP acepta el mensaje y el proceso cae antes de confirmar `sent_at`, el siguiente intento puede duplicarlo. La aceptación SMTP tampoco garantiza entrega al buzón. El dispatcher procesa lotes de hasta 20 con bloqueo de filas y límites de tiempo SMTP de cinco segundos; no es una infraestructura de correo de alto volumen.

## Verificación

`./mvnw -B test` ejecuta pruebas existentes y nuevas sobre H2, sin conectarse a MySQL ni enviar correo real. Las nuevas clases cubren servicio, contrato HTTP, seguridad con repositorios reales, envío/reintento con un transportador simulado, rollback de solicitud y correo, envío posterior al commit y decisiones concurrentes. Resultado: 70 pruebas correctas, 43 nuevas y 27 existentes.

Antes de desplegar: aplicar el esquema, cargar asignaciones y verificar las credenciales/remitente en un entorno de correo controlado. Esta implementación no aplica automáticamente el SQL ni altera datos existentes. No cambia el frontend mock; su conexión requiere una fase posterior porque el contrato ahora usa fechas/horas separadas.

## Archivos de esta entrega

Modificado: `pom.xml` (starter de correo administrado por la versión existente de Spring Boot).

Creados:

- `src/main/java/com/backendintranet/config/AbsenceMailConfig.java`
- `src/main/java/com/backendintranet/controller/AbsenceController.java`
- `src/main/java/com/backendintranet/controller/AbsenceExceptionHandler.java`
- `src/main/java/com/backendintranet/dto/request/AbsenceCreateRequest.java`
- `src/main/java/com/backendintranet/dto/request/AbsenceDecisionRequest.java`
- `src/main/java/com/backendintranet/dto/response/AbsenceResponse.java`
- `src/main/java/com/backendintranet/entity/AbsenceApproverAssignment.java`
- `src/main/java/com/backendintranet/entity/AbsenceMail.java`
- `src/main/java/com/backendintranet/entity/AbsenceRequest.java`
- `src/main/java/com/backendintranet/entity/AbsenceStatus.java`
- `src/main/java/com/backendintranet/exception/AbsenceConflictException.java`
- `src/main/java/com/backendintranet/repository/AbsenceApproverAssignmentRepository.java`
- `src/main/java/com/backendintranet/repository/AbsenceMailRepository.java`
- `src/main/java/com/backendintranet/repository/AbsenceRequestRepository.java`
- `src/main/java/com/backendintranet/service/AbsenceService.java`
- `src/main/java/com/backendintranet/service/impl/AbsenceMailDispatcher.java`
- `src/main/java/com/backendintranet/service/impl/AbsenceServiceImpl.java`
- `src/test/java/com/backendintranet/config/security/AbsenceSecurityIntegrationTest.java`
- `src/test/java/com/backendintranet/controller/AbsenceControllerContractTest.java`
- `src/test/java/com/backendintranet/service/AbsenceMailDispatcherTest.java`
- `src/test/java/com/backendintranet/service/AbsenceServiceImplTest.java`
- `src/test/java/com/backendintranet/service/AbsenceTransactionIntegrationTest.java`
- `docs/sql/absence-management.sql`
- `docs/absence-management.md`
