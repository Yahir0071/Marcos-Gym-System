# Documentación de Aportes - Jeferson Alonso Almerco Ramírez
**Rol:** Seguridad, Autenticación y Autorización, Control de Cuentas, JWT y Configuración (Semana 15 - Sesión 30)
**Rama:** `LaRamitaDeAlonso`
**Fecha de Actualización:** 18 de Noviembre de 2026

---

## 1. Resumen de Implementación de los 4 Puntos Finales

A continuación se detalla la resolución técnica de los 4 puntos requeridos para el merge a `main`:

### Punto 1. PostgreSQL: Migración de `usuarios_rol_check` y Compatibilidad con `main`
* **Problema:** En el esquema previo de la rama `main`, la tabla `usuarios` contaba con la restricción `CHECK (rol IN ('ADMINISTRADOR', 'CLIENTE'))`. Al intentar insertar un `GESTOR_TECNICO` o iniciar la aplicación con cuentas heredadas con `rol = 'ADMINISTRADOR'`, el motor PostgreSQL/H2 generaba violaciones de integridad `JdbcSQLIntegrityConstraintViolationException`.
* **Solución Implementada (`DataInitializerConfig.java`):**
  1. Se agregó un runner con `@Order(1)` que elimina la restricción check previa (`ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check`).
  2. Se ejecuta la migración nativa de datos: `UPDATE usuarios SET rol = 'ADMINISTRACION' WHERE rol = 'ADMINISTRADOR'`.
  3. Se asegura la columna `token_version` con valor por defecto `1`.
  4. Se crea la nueva restricción con los 4 roles del sistema: `ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('CLIENTE', 'RECEPCION', 'ADMINISTRACION', 'GESTOR_TECNICO'))`.
* **Verificación de prueba:** En `SecurityAlonsoTests.testMigracionSqlEsquemaMain`, se replica exactamente la estructura de `main` con la restricción heredada, se comprueba que `GESTOR_TECNICO` no podía crearse bajo `main`, se ejecuta la migración, y se valida que la cuenta pasa a `ADMINISTRACION` y la creación de `GESTOR_TECNICO` se realiza con éxito.

---

### Punto 2. Pantallas: Alineación de Controles Visuales y Permisos Operativos
* **Productos ([productos.html](file:///c:/Users/PC/Downloads/Marcos-Gym-System/src/main/resources/templates/admin/productos.html)):**
  - Botones `EDITAR` y `DESACTIVAR/ACTIVAR` en cada fila envueltos en `sec:authorize="hasRole('ADMINISTRACION')"`.
  - Si el rol es `RECEPCION`, se muestra la etiqueta informativa `SOLO LECTURA` y se oculta el botón superior `+ NUEVO PRODUCTO`.
* **Socios ([socios.html](file:///c:/Users/PC/Downloads/Marcos-Gym-System/src/main/resources/templates/admin/socios.html)):**
  - Botón de acción `VER / EDITAR` envuelto en `sec:authorize="hasRole('RECEPCION')"`.
  - Si el rol es `ADMINISTRACION`, visualiza `SOLO LECTURA` y no puede ver ni abrir `+ REGISTRAR SOCIO`.
* **Pagos y Boletas ([pagos.html](file:///c:/Users/PC/Downloads/Marcos-Gym-System/src/main/resources/templates/admin/pagos.html)):**
  - Botón `ENTREGAR` de pedidos en línea restringido con `sec:authorize="hasRole('RECEPCION')"`.
  - Botón `+ REGISTRAR PAGO EN CAJA` restringido a `RECEPCION`.
  - `ADMINISTRACION` mantiene acceso exclusivo de auditoría y consulta de reportes y boletas sin facultades de entrega física de mercadería.

---

### Punto 3. Seguridad: Compras, Boletas y Revocación Persistente tras Reinicio
* **Permisos de Compras y Boletas:**
  - `SecurityConfig.java` y `CompraClienteController.java` (`@PreAuthorize`):
    - `POST /cliente/api/compras`: Exclusivo de `ROLE_CLIENTE`. El `GESTOR_TECNICO` está estrictamente bloqueado (403 Forbidden).
    - `GET /cliente/boleta-venta/descargar` y vistas de boleta: Habilitado para `CLIENTE`, `RECEPCION` y `ADMINISTRACION`. El `GESTOR_TECNICO` no puede acceder a ningún comprobante financiero.
* **Persistencia de Revocación JWT tras Reinicio:**
  - Se agregó la columna `token_version` en la tabla `usuarios` y en la entidad `Usuario.java`.
  - En `SecurityTokenRevocationManager.java`, cada invocación a `revocarAccesoUsuario(username)` incrementa y persiste el nuevo `token_version` en la base de datos PostgreSQL.
  - Al reiniciar el servidor Spring Boot (o vaciar la memoria caché con `limpiarCacheMemoria()`), el sistema valida los JWT consultando la versión persistida en la BD. Si un JWT presenta una versión previa (`tokenVersion = 1` frente a la nueva `token_version = 2`), se rechaza de inmediato con HTTP 401 Unauthorized.
* **Revocación en APIs que Usan Sesión:**
  - `SessionValidityFilter.java` intercepta peticiones API autenticadas por cookies de sesión (sin Bearer).
  - Si la cuenta del usuario fue desactivada (`activo = false`) o su rol fue modificado en la base de datos, invalida la sesión de inmediato y responde con HTTP 401 Unauthorized JSON:
    `{"error":"UNAUTHORIZED","mensaje":"La cuenta ha sido desactivada."}` en lugar de permitir la acción o efectuar una redirección HTML 302.

---

### Punto 4. Pruebas Automatizadas y Documentación
* Todas las 51 pruebas automatizadas del proyecto pasan al 100% de forma limpia:
  - 13 pruebas de seguridad y RBAC en [SecurityAlonsoTests.java](file:///c:/Users/PC/Downloads/Marcos-Gym-System/src/test/java/pe/edu/utp/Grupo04/security/SecurityAlonsoTests.java).
  - 38 pruebas de servicios, validadores y modelos existentes.
* Cobertura de regresión añadida:
  - `testMigracionSqlEsquemaMain`: Validación contra el esquema check constraint de `main`.
  - `testRestriccionesOperativasPorRol`: Verificación de bloqueo 403 Forbidden a modificaciones cruzadas de Productos, Socios y Entrega de pedidos.
  - `testPermisosComprasYBoletasPorRol`: Aislamiento del Gestor Técnico en boletas y compras.
  - `testJwtRevocadoSigueInvalidoTrasReinicio`: Verificación de revocación persistente de tokens tras reinicio.
  - `testRevocacionEnApisQueUsanSesion`: Respuesta 401 JSON ante llamadas API con sesión invalidada.

---

## 2. Instrucciones para Configurar y Ejecutar

### Variables de Entorno
```bash
# Conexión PostgreSQL
export DB_URL="jdbc:postgresql://<host>:5432/<db>?sslmode=require"
export DB_USERNAME="<usuario>"
export DB_PASSWORD="<password>"

# Clave Secreta JWT (Semana 15 - M11: Sin contraseñas de respaldo en código)
export JWT_SECRET="<clave aleatoria privada>"
export JWT_EXPIRATION_MS="86400000"

# Aprovisionamiento Opcional del Gestor Técnico Inicial
export TECNICO_INITIAL_USERNAME="<correo de la cuenta inicial>"
export TECNICO_INITIAL_PASSWORD="<contraseña privada>"
```

### Ejecución de Pruebas
```bash
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\mvnw.cmd test
```

### Arranque del Servidor Local
```bash
.\mvnw.cmd spring-boot:run
```
Acceso web: `http://localhost:8080/inicio`

### Actualizar una base existente con el perfil prod

Ejecuta `database/migrar_roles_alonso.sql` en PostgreSQL antes del primer arranque con `ddl-auto=validate`. En desarrollo con `ddl-auto=update`, la migración se ejecuta durante el arranque. El aprovisionamiento inicial solo crea una cuenta si no hay un gestor activo y el correo configurado está libre.
