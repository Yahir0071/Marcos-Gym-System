# Energy Gym — Sistema de Gestión Integral (`Grupo04_MW_Gym`)

Aplicación web para la gestión integral de **Energy Gym** (Sede Av. Las Américas, Ate — Lima), desarrollada con **Java 21** y **Spring Boot 4.1.1**.

---

## Stack Tecnológico

- **Backend:** Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Security)
- **Autenticación y Seguridad:** Sesiones con protección CSRF en vistas web + API REST stateless con JWT (`jjwt 0.12.6`) y revocación por versión de token
- **Base de Datos:** PostgreSQL (Supabase) en desarrollo/producción · H2 en memoria para pruebas
- **Frontend:** Thymeleaf, Bootstrap 5.3, FontAwesome 6, JavaScript modular (ES6)

---

## Roles del Sistema (RBAC)

| Rol | Alcance principal |
| :--- | :--- |
| **`CLIENTE`** | Portal de socio: mi membresía, rutina semanal, carnet digital QR, congelamiento, reservas, tienda de suplementos, compras y Coach IA. |
| **`RECEPCION`** | Registro y edición de socios, asignación de membresías, cobros presenciales, entrega de pedidos, atención de reservas y registro de afluencia. |
| **`ADMINISTRACION`** | Dashboard ejecutivo, gestión de planes, promociones, inventario de productos, equipos y mantenimientos. |
| **`GESTOR_TECNICO`** | Administración de cuentas internas, cambio de roles, revocación de accesos y auditoría (aislado de operaciones comerciales). |

---

## Funcionalidades Destacadas

- **Planes y beneficios dinámicos (`BeneficioPlan`):** tarifas en modalidades `MENSUAL`, `TRIMESTRAL (-10%)` y `ANUAL (-20%)` con catálogo de beneficios configurable desde `/admin/planes` y tabla comparadora dinámica en `/planes`.
- **Control de acceso QR + Afluencia:** emisión de QR único por servidor (`GET /cliente/api/carnet/qr`), validación con horario de sede (`Lun–Vie 06:00–23:00`, `Sáb 07:00–20:00`, `Dom 08:00–14:00`, zona `America/Lima`), *cooldown* de 60 minutos por socio e incremento atómico en `registros_afluencia` (tope 80).
- **Chatbot Freemium + Coach IA:**
  - **Visitantes / sin plan activo:** motor local con datos reales de BD (planes, promociones, horarios, afluencia, suplementos) sin costo de API.
  - **Socios con membresía `ACTIVA`:** Coach IA vía Gemini HTTP (`GEMINI_API_KEY`) con contexto del socio, pre-filtro anti-*off-topic*, límite diario configurable y *fallback* automático offline.

---

## Configuración y Ejecución Local

1. **Crear archivo de variables de entorno:**
   Copia `.env.example` como `.env` en la raíz del proyecto (junto a `pom.xml`). El archivo `.env` está ignorado en `.gitignore`.

   ```properties
   DB_URL=jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME=postgres.<tu_proyecto>
   DB_PASSWORD=<tu_password>
   JWT_SECRET=<clave_secreta_minimo_32_caracteres>
   ```

   *(Opcional: configura `GEMINI_API_KEY` para habilitar el Coach IA en vivo; sin ella funciona el fallback local sin errores).*

2. **Ejecutar la aplicación:**
   - Desde **IntelliJ IDEA:** ejecutar `Grupo04MwGymApplication` (Spring Boot carga `.env` automáticamente mediante `spring.config.import=optional:file:.env[.properties]`).
   - Desde **terminal (Windows):**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```

3. **Ejecutar pruebas automatizadas:**
   ```powershell
   .\mvnw.cmd test
   ```
   *(Las pruebas usan H2 en memoria configurado en `src/test/resources/application.properties` sin tocar Supabase).*

---

## Migraciones SQL

Los scripts en `src/main/resources/db/migration/` (`V2`–`V10`) y `database/migrar_roles_alonso.sql` documentan la evolución del esquema para entornos con `spring.jpa.hibernate.ddl-auto=validate` (perfil `prod`). En desarrollo (`ddl-auto=update`), Hibernate sincroniza el esquema automáticamente.
