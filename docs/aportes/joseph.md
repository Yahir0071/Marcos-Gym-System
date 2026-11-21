# Aporte de Joseph

## Interfaz y reportes

- Se actualizaron los menús según los roles de `main`, las vistas móviles, los controles de formulario y los mensajes de simulación académica.
- El Dashboard y Afluencia usan los registros del sistema para sus indicadores.
- En Socios, Recepción edita mediante la API de operaciones. La pantalla de edición del servidor también está disponible desde el enlace si no se carga JavaScript.
- El cliente puede enviar una solicitud de congelamiento y consultar su estado. La solicitud queda pendiente de revisión.

## Migración

Ejecutar `src/main/resources/db/migration/V3__joseph_congelamiento.sql` en PostgreSQL antes del primer arranque con el perfil `prod` y `ddl-auto=validate`. El archivo crea la tabla de solicitudes y su índice de consulta.

Los perfiles de redes sociales necesitan URLs verificadas antes de publicarse. Las capturas del aporte están en [evidencias-joseph](evidencias-joseph/).
