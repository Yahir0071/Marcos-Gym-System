# Informe de Aporte Individual - Módulo de Operaciones, Validaciones y CRUD

* **Estudiante:** Chancan Moreno, Esaul Ronaldo Soto  
* **Curso:** Marcos de Desarrollo Web  
* **Grupo:** Grupo 04 - Energy Gym  
* **Rama de Git:** `tarea/esaul-operaciones-validaciones` (o `LaRamazaDeEsaul`)  
* **Corte Académico:** Semana 15, Sesión 30 (Semanas 9 a 12 del sílabo: JPA, CRUD, Spring Validator e Integridad)  
* **Revisor de PR asignado:** Jacay Kennedy, Joseph  

---

## 1. Resumen Ejecutivo de Hallazgos QA Resueltos

Se implementaron y corrigieron de forma verificable los **9 hallazgos** asignados en la auditoría técnica y plan de distribución:

| Código | Nivel | Componente Afectado | Descripción del Fallo Original | Solución Técnica Implementada |
| :--- | :--- | :--- | :--- | :--- |
| **C08** | **CRÍTICO** | `SocioController`, `SocioService`, `Socio` | La edición y eliminación de socios solo modificaba visualmente el DOM; al recargar la página los cambios desaparecían. | Se crearon endpoints persistentes MVC y REST (`POST /admin/socios/editar/{id}`, `PUT /api/admin/socios/{id}` y cambio de estado) que validan y persisten en BD mediante JPA. |
| **C03** | **CRÍTICO** | `ReservaController`, `ReservaService` | Un cliente (QA1) podía enviar peticiones para cancelar la reserva perteneciente a otro socio (QA2). | Se implementó validación estricta de propiedad contra el usuario autenticado en la sesión (`Authentication.getName()`), arrojando `AccesoDenegadoException` (403) ante intentos no autorizados. |
| **C09** | **CRÍTICO** | `HomeController`, `MembresiaService` | La contratación online de un plan anual (12 meses) solo otorgaba 30 días de vigencia en BD. | Se ajustó el cálculo de vigencia en el servidor para soportar planes `ANUAL` (+1 año), respetando los meses contratados y encadenando renovaciones sobre membresías activas previas. |
| **C06** | **MEDIO** | `MembresiaController` (`/api/torniquete/validar`) | El acceso por QR concedía pase (`CONCEDIDO`) a socios sin membresía o con membresía vencida, mediante tokens ficticios. | Se implementó validación en el servidor que comprueba que el socio cuente obligatoriamente con una membresía en estado `ACTIVA` y con `fechaFin >= hoy`. Se delimitó formalmente como simulación académica. |
| **M01** | **MEDIO** | `HomeController` (`/registrarse`) | El registro público aceptaba nombres de solo espacios, contraseñas de espacios, correos no válidos y no validaba confirmación de contraseña ni persistía género/fecha de nacimiento. | Se normalizaron entradas con `.trim()`, validación de formato `@Email`, longitud de DNI (8 dígitos) y teléfono (9 dígitos), validación de contraseñas coincidentes y persistencia de `genero` y `fechaNacimiento` en la entidad `Socio`. |
| **M02** | **MEDIO** | `GlobalExceptionHandler`, DTOs | Entradas inválidas (ej. texto de 5,000 caracteres) causaban errores genéricos `HTTP 500`. | Se creó un `@ControllerAdvice` global que intercepta `MethodArgumentNotValidException`, `BindException` y `ConstraintViolationException`, retornando códigos `400 Bad Request` y `422` claros en español. Se acotaron los campos con `@Size` acordes a la BD. |
| **M03** | **MEDIO** | `PromocionDTO`, `PromocionService` | Se permitía guardar descuentos absurdos (ej. 999%) y fechas de fin anteriores a la de inicio. | Se incorporaron restricciones `@DecimalMin("0.01")` y `@DecimalMax("100.00")` en `PromocionDTO`, y validación en `PromocionServiceImpl` que exige que `fechaFin` sea igual o posterior a `fechaInicio`. |
| **M04** | **MEDIO** | `PlanServiceImpl`, `PromocionServiceImpl`, `ProductoServiceImpl`, `SocioServiceImpl` | Operaciones sobre IDs inexistentes (ej. cambiar estado) anunciaban éxito en lugar de reportar error. | Se reemplazó el uso de `.ifPresent(...)` silencioso por lanzamiento de `RecursoNoEncontradoException` (404), garantizando que nunca se confirme una operación sobre registros inexistentes. |
| **M12** | **MEDIO** | `ProductoRepository`, `ProductoService`, `ReservaService` | Actualizaciones de inventario sin atomicidad y posibilidad de doble cancelación de reserva devolviendo stock duplicado. | Se crearon consultas atómicas condicionales `@Modifying @Query` en `ProductoRepository` (`descontarStockAtomico` y `restaurarStockAtomico`) y máquina de estados en `ReservaService` que impide cancelaciones repetidas. |

---

## 2. Contratos de Interfaz Acordados (para consumo de Joseph)

Para que el frontend dinamizado por Joseph se conecte a las operaciones de backend sin generar conflictos, se dejaron disponibles las siguientes rutas:

### A. Edición y Desactivación de Socios (C08)

#### 1. Endpoint Web Form (Thymeleaf / Redirect):
* **Ruta:** `POST /admin/socios/editar/{id}`
* **Parámetros:** `nombres`, `apellidos`, `telefono`, `activo`
* **Comportamiento:** Redirige a `/admin/socios` con flash attribute `mensajeExito` o `mensajeError`.

#### 2. Endpoint REST JSON (AJAX / Fetch en `tables-crud.js`):
* **Ruta:** `PUT /api/admin/socios/{id}`
* **Headers:** `Content-Type: application/json`, `Accept: application/json`
* **Body Request:**
```json
{
  "nombres": "Carlos Eduardo",
  "apellidos": "Mendoza Rojas",
  "telefono": "987654321",
  "activo": true
}
```
* **Respuesta Exitosa (HTTP 200):**
```json
{
  "success": true,
  "mensaje": "Socio actualizado exitosamente en base de datos.",
  "socio": {
    "id": 1,
    "nombres": "Carlos Eduardo",
    "apellidos": "Mendoza Rojas",
    "telefono": "987654321",
    "activo": true
  }
}
```
* **Respuesta Error (HTTP 400 / 404):**
```json
{
  "success": false,
  "mensaje": "Datos de entrada inválidos o incompletos",
  "errores": {
    "nombres": "Los nombres son obligatorios",
    "telefono": "El teléfono debe contener exactamente 9 dígitos numéricos"
  }
}
```

#### 3. Endpoint de Cambio de Estado / Baja Lógica:
* **Ruta:** `PATCH /api/admin/socios/{id}/estado?activo=false` o `POST /admin/socios/cambiar-estado`
* **Respuesta Exitosa:**
```json
{
  "success": true,
  "mensaje": "Socio desactivado correctamente",
  "activo": false
}
```

---

## 3. Diccionario de Datos Actualizado (Módulo Socios)

Se extendió el modelo de datos para la entidad `Socio` (`socios` en PostgreSQL/Supabase):

| Campo | Tipo SQL | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | Identificador único del socio. |
| `dni` | `VARCHAR(8)` | `NOT NULL, UNIQUE` | Documento Nacional de Identidad (8 dígitos numéricos). |
| `nombres` | `VARCHAR(255)` | `NOT NULL` | Nombres del socio (normalizado, sin espacios extra). |
| `apellidos` | `VARCHAR(255)` | `NOT NULL` | Apellidos paterno y materno del socio. |
| `telefono` | `VARCHAR(20)` | `NULLABLE` | Teléfono móvil de contacto (9 dígitos). |
| `genero` | `VARCHAR(20)` | `NULLABLE` | Género del socio (MASCULINO, FEMENINO, OTRO). |
| `fecha_nacimiento` | `DATE` | `NULLABLE` | Fecha de nacimiento para cálculo de edad/perfil deportivo. |
| `usuario_id` | `BIGINT` | `FOREIGN KEY (usuarios.id)` | Enlace a la cuenta de autenticación y credenciales. |

Script de migración generado: `src/main/resources/db/migration/V2__esaul_operaciones_validaciones.sql`

---

## 4. Evidencia de Ejecución de Pruebas Automatizadas

Se crearon y actualizaron suites completas de pruebas unitarias con **JUnit 5** y **Mockito**, alcanzando **26 pruebas exitosas en los módulos del responsable** y **49 pruebas exitosas en todo el proyecto**:

```bash
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running pe.edu.utp.Grupo04.MembresiaServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0  -- (Incluye C09: 12 meses anual y renovación encadenada)
[INFO] Running pe.edu.utp.Grupo04.ProductoServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0  -- (Incluye M12: Descuento y restauración atómica)
[INFO] Running pe.edu.utp.Grupo04.PromocionServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0  -- (Incluye M03: Rechazo de >100% y fechas invertidas)
[INFO] Running pe.edu.utp.Grupo04.ReservaServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0  -- (Incluye C03: Rechazo de reserva ajena y M12: Idempotencia)
[INFO] Running pe.edu.utp.Grupo04.SocioServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0  -- (Incluye C08: Persistencia en edición y baja lógica)
[INFO] 
[INFO] Results:
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 5. Fragmento Redactado para el Informe Word Final

*(Este texto está listo para ser copiado al capítulo correspondiente del informe académico general).*

### Sección: Gestión Operativa, Integridad Transaccional y Validaciones (Semanas 9 a 12)

#### 1. Persistencia y Ciclo de Vida del Socio (RF02 / C08)
El sistema implementa el ciclo de vida completo de los socios del gimnasio mediante la arquitectura en capas (Controlador, Servicio y Repositorio JPA). La edición de socios (`actualizarSocio`) valida la integridad de los datos personales (nombres, apellidos y teléfono con formato estándar) y asegura su persistencia física en PostgreSQL. La baja de socios se realiza bajo el patrón de desactivación lógica (`cambiarEstado(false)`), preservando el historial de asistencias y membresías asociadas pero revocando de inmediato el acceso al sistema.

#### 2. Control de Acceso y Propiedad sobre Reservas (RF07 / C03 / M12)
Para garantizar la confidencialidad e integridad operativa, las cancelaciones de reservas en el portal del cliente verifican estrictamente la propiedad del registro contra el contexto de seguridad (`Authentication`). Si un usuario intenta enviar un identificador ajeno, el sistema aborta la transacción con una excepción de acceso denegado (HTTP 403). Asimismo, las operaciones de descuento y restitución de stock se ejecutan mediante sentencias SQL condicionales y atómicas a nivel de motor de base de datos (`descontarStockAtomico` y `restaurarStockAtomico`), impidiendo condiciones de carrera (*race conditions*) y garantizando que una cancelación duplicada no reintegre existencias de forma indebida.

#### 3. Reglas de Negocio en Membresías y Promociones (C06 / C09 / M03)
La contratación de planes calcula de manera determinista la vigencia en el servidor: los planes anuales acreditan formalmente 12 meses (365 días), y las renovaciones detectan si el socio cuenta con tiempo remanente para iniciar la nueva vigencia a partir del vencimiento anterior. La validación del pase QR verifica en tiempo real la existencia de una membresía activa y vigente, denegando el ingreso si el usuario carece de plan o este se encuentra vencido. Finalmente, el módulo de promociones aplica restricciones declarativas que limitan el porcentaje de descuento a valores entre 1% y 100%, exigiendo coherencia cronológica entre la fecha de inicio y la fecha de finalización.
