# Copia de notificaciones de Ausentismo

Cada creación, aprobación y rechazo conserva el destinatario original y registra
una segunda entrada en `absence_mail_outbox` con el mismo asunto y cuerpo.
Si las direcciones coinciden, ignorando mayúsculas y espacios exteriores, solo
se registra el correo original. Los reintentos siguen en el dispatcher existente;
cada entrada tiene su propio contador, próxima fecha de intento y resultado.
No cambia el esquema ni la lógica de aprobación. No modifica correos ya encolados.

## Configuración

`ABSENCE_MAIL_HR_RECIPIENT` configura la copia y su valor predeterminado es
`talentohumano@licodistribuciones.com`. Una configuración vacía también utiliza
el predeterminado. Se conserva la configuración SMTP actual de `absence.mail`.
El envío requiere que `absence.mail.enabled` esté habilitado.

## Despliegue pendiente de autorización

No se encontró `/opt/intranet` en el equipo de desarrollo ni una configuración
SSH que permita identificar el servidor. El repositorio contiene un Dockerfile,
pero no contiene Docker Compose ni identifica el servicio de producción.
Por tanto, no es posible proporcionar todavía comandos de actualización y
reinicio adaptados al servidor sin inventar nombres.

Después de aprobar y fusionar el PR, y autorizar expresamente producción:

1. En el servidor, ejecutar estas consultas de inventario:

   ```bash
   cd /opt/intranet
   pwd
   find . -maxdepth 3 -type f \( -name 'compose*.yml' -o -name 'compose*.yaml' -o -name 'docker-compose*.yml' -o -name 'docker-compose*.yaml' -o -name 'Dockerfile' \) -print
   docker compose ls
   docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
   systemctl list-units --type=service --all --no-pager
   ```

2. Identificar el backend, su origen de código o imagen, archivo de entorno,
   mecanismo de despliegue y endpoint de salud. Respaldar los archivos reales de
   configuración con permisos restringidos y registrar la imagen o revisión
   anterior para rollback. No imprimir credenciales mediante `docker inspect`,
   `docker compose config`, archivos de entorno o logs.
3. Verificar SMTP y agregar `ABSENCE_MAIL_HR_RECIPIENT` a la configuración del
   backend si se desea sobrescribir el valor predeterminado. Si usa Compose,
   confirmar que la variable se entrega al contenedor mediante `environment`
   o `env_file`; definirla en `.env` por sí solo no garantiza su entrega.
4. Actualizar y recrear únicamente el servicio backend identificado, sin reiniciar
   dependencias. No ejecutar `docker compose down`, borrar volúmenes ni cambiar
   MySQL, Nginx, frontend u otros módulos. No se necesita migración de base de datos.
5. Comprobar el endpoint de salud real y logs del backend. Realizar una prueba
   SMTP controlada sin crear solicitudes reales, preferiblemente con un entorno
   aislado y datos sintéticos. Confirmar entrega a ambos buzones y verificar que
   una falla conserva únicamente el destinatario afectado para reintento.
6. Si falla la validación, restaurar la imagen y configuración previas del backend
   conservando la base de datos y la cola.

Las pruebas automatizadas usan H2 y un emisor simulado; no envían correos reales.
La comprobación de entrega SMTP real permanece pendiente del acceso y autorización.

## Validación del cambio

`./mvnw test`: 76 pruebas, cero fallos, cero errores y cero omitidas.
Incluye creación/aprobación/rechazo con copia idéntica, deduplicación de los tres
 eventos, destinatario configurable, rollback de ambas entradas, decisiones
concurrentes y reintento independiente tras fallas de cualquiera de los buzones.
La ejecución requirió salir del sandbox para permitir la conexión de Mockito a
la JVM. `git diff --check` también pasó.
