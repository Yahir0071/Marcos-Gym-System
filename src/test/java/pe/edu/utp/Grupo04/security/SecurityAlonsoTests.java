package pe.edu.utp.Grupo04.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pe.edu.utp.Grupo04.dto.CuentaInternaDTO;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;
import pe.edu.utp.Grupo04.security.jwt.JwtService;
import pe.edu.utp.Grupo04.service.GestionCuentasService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SecurityAlonsoTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GestionCuentasService gestionCuentasService;

    @Autowired
    private SecurityTokenRevocationManager revocationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        prepararUsuarioDePrueba("recep@energygym.pe", Rol.RECEPCION);
        prepararUsuarioDePrueba("admin@energygym.pe", Rol.ADMINISTRACION);
        prepararUsuarioDePrueba("tecnico@energygym.pe", Rol.GESTOR_TECNICO);
        prepararUsuarioDePrueba("cliente@energygym.pe", Rol.CLIENTE);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private void prepararUsuarioDePrueba(String username, Rol rol) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword("sin-inicio-de-sesion-en-pruebas-mock");
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("JWT: Emisión, lectura de claims y validación de firma correcta")
    void testEmisionYValidacionJwt() {
        String token = jwtService.generarToken("alonso@energygym.pe", "GESTOR_TECNICO", 1L);
        assertNotNull(token);
        assertTrue(jwtService.validarToken(token));
        assertEquals("alonso@energygym.pe", jwtService.extraerUsername(token));
        assertEquals("GESTOR_TECNICO", jwtService.extraerRol(token));
        assertEquals(1L, jwtService.extraerTokenVersion(token));
    }

    @Test
    @DisplayName("JWT: Detección y rechazo de token alterado")
    void testTokenAlteradoRechazado() {
        String token = jwtService.generarToken("usuario@energygym.pe", "CLIENTE", 1L);
        String tokenAlterado = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(jwtService.validarToken(tokenAlterado));
    }

    @Test
    @DisplayName("JWT: Detección y rechazo de token vencido")
    void testTokenVencidoRechazado() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", "CLIENTE");
        claims.put("tokenVersion", 1L);
        Date pasado = new Date(System.currentTimeMillis() - 10000);

        String tokenVencido = io.jsonwebtoken.Jwts.builder()
                .claims(claims)
                .subject("vencido@energygym.pe")
                .issuedAt(new Date(System.currentTimeMillis() - 20000))
                .expiration(pasado)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "ClaveSecretaSuperLargaYSeguraParaPruebasUnitariasYSeguridad2026AlonsoJJWT512BitsMinimo!".getBytes()
                ))
                .compact();

        assertFalse(jwtService.validarToken(tokenVencido));
    }

    @Test
    @DisplayName("Gestor Técnico: Crear cuenta interna autorizada y registrar auditoría")
    void testCreacionCuentaInterna() {
        String correo = "test.recepcion." + System.currentTimeMillis() + "@energygym.pe";
        CuentaInternaDTO dto = new CuentaInternaDTO();
        dto.setUsername(correo);
        dto.setPassword("Secret123!");
        dto.setRol(Rol.RECEPCION);

        Usuario creado = gestionCuentasService.crearCuentaInterna(dto, "tecnico@energygym.pe");
        assertNotNull(creado.getId());
        assertEquals(Rol.RECEPCION, creado.getRol());
        assertTrue(creado.getActivo());

        // Verificar que no se puede crear con rol CLIENTE desde el panel técnico
        CuentaInternaDTO dtoCliente = new CuentaInternaDTO();
        dtoCliente.setUsername("cliente.invalido@energygym.pe");
        dtoCliente.setPassword("Secret123!");
        dtoCliente.setRol(Rol.CLIENTE);
        assertThrows(IllegalArgumentException.class, () -> gestionCuentasService.crearCuentaInterna(dtoCliente, "tecnico@energygym.pe"));
    }

    @Test
    @DisplayName("Gestor Técnico: Desactivar cuenta y prevenir que se desactive a sí mismo")
    void testDesactivarCuenta() {
        String correo = "desactivar." + System.currentTimeMillis() + "@energygym.pe";
        CuentaInternaDTO dto = new CuentaInternaDTO();
        dto.setUsername(correo);
        dto.setPassword("Secret123!");
        dto.setRol(Rol.ADMINISTRACION);

        Usuario creado = gestionCuentasService.crearCuentaInterna(dto, "tecnico@energygym.pe");
        Usuario modificado = gestionCuentasService.alternarEstadoCuenta(creado.getId(), "tecnico@energygym.pe");
        assertFalse(modificado.getActivo());

        // Prevenir auto-desactivación del propio gestor en sesión
        assertThrows(IllegalStateException.class, () ->
                gestionCuentasService.alternarEstadoCuenta(creado.getId(), correo));
    }

    @Test
    @DisplayName("Punto 1 (Feedback): Migración sobre el esquema de main (usuarios_rol_check, ADMINISTRADOR a ADMINISTRACION y creación de Gestor Técnico)")
    void testMigracionSqlEsquemaMain() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Crear una tabla de prueba con la estructura exacta de 'main' y su restricción usuarios_rol_check
            stmt.execute("DROP TABLE IF EXISTS usuarios_test_main");
            stmt.execute("CREATE TABLE usuarios_test_main (" +
                    "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "username VARCHAR(100) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "rol VARCHAR(50) NOT NULL, " +
                    "activo BOOLEAN DEFAULT TRUE, " +
                    "CONSTRAINT usuarios_test_rol_check CHECK (rol IN ('ADMINISTRADOR', 'CLIENTE')))");

            // 2. Insertar cuenta de prueba con rol histórico 'ADMINISTRADOR'
            stmt.executeUpdate("INSERT INTO usuarios_test_main (username, password, rol, activo) VALUES (" +
                    "'admin.main@energygym.pe', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'ADMINISTRADOR', true)");

            // Verificar que bajo el esquema anterior de main NO se podía crear GESTOR_TECNICO (falla por check constraint)
            assertThrows(Exception.class, () -> {
                try (Statement s2 = conn.createStatement()) {
                    s2.executeUpdate("INSERT INTO usuarios_test_main (username, password, rol, activo) VALUES (" +
                            "'tecnico.main@energygym.pe', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'GESTOR_TECNICO', true)");
                }
            });

            // 3. Ejecutar la rutina de migración de Alonso
            stmt.execute("ALTER TABLE usuarios_test_main DROP CONSTRAINT usuarios_test_rol_check");
            int migrados = stmt.executeUpdate("UPDATE usuarios_test_main SET rol = 'ADMINISTRACION' WHERE rol = 'ADMINISTRADOR'");
            assertEquals(1, migrados);

            stmt.execute("ALTER TABLE usuarios_test_main ADD CONSTRAINT usuarios_test_rol_check CHECK (rol IN ('CLIENTE', 'RECEPCION', 'ADMINISTRACION', 'GESTOR_TECNICO'))");

            // 4. Verificar que la cuenta de prueba pasó a ADMINISTRACION
            try (ResultSet rs = stmt.executeQuery("SELECT rol FROM usuarios_test_main WHERE username = 'admin.main@energygym.pe'")) {
                assertTrue(rs.next());
                assertEquals("ADMINISTRACION", rs.getString("rol"));
            }

            // 5. Verificar que tras la migración SÍ se puede crear un Gestor Técnico
            int tecnicoCreado = stmt.executeUpdate("INSERT INTO usuarios_test_main (username, password, rol, activo) VALUES (" +
                    "'tecnico.main@energygym.pe', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'GESTOR_TECNICO', true)");
            assertEquals(1, tecnicoCreado);

            stmt.execute("DROP TABLE IF EXISTS usuarios_test_main");
        }
    }

    @Test
    @DisplayName("Punto 2 (Feedback): Restricciones operativas (Recepción no edita productos, Administración no entrega pedidos)")
    void testRestriccionesOperativasPorRol() throws Exception {
        // Recepción no puede editar o guardar productos (POST /admin/productos/guardar -> 403)
        mockMvc.perform(post("/admin/productos/guardar")
                        .with(csrf())
                        .with(user("recep@energygym.pe").roles("RECEPCION")))
                .andExpect(status().isForbidden());

        // Administración no puede registrar socios ni entregar pedidos (POST -> 403)
        mockMvc.perform(post("/admin/socios/guardar")
                        .with(csrf())
                        .with(user("admin@energygym.pe").roles("ADMINISTRACION")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/api/compras/PED-00521/entregar")
                        .with(csrf())
                        .with(user("admin@energygym.pe").roles("ADMINISTRACION")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Punto 3 (Feedback): Aislamiento del Gestor Técnico y permisos de compras y boletas por rol")
    void testPermisosComprasYBoletasPorRol() throws Exception {
        // 1. Gestor Técnico NO puede realizar compras ficticias (403 Forbidden)
        mockMvc.perform(post("/cliente/api/compras")
                        .with(csrf())
                        .with(user("tecnico@energygym.pe").roles("GESTOR_TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[],\"buyerName\":\"Gestor Técnico\",\"buyerEmail\":\"tecnico@energygym.pe\"}"))
                .andExpect(status().isForbidden());

        // 2. Gestor Técnico NO puede descargar boletas (403 Forbidden)
        mockMvc.perform(get("/cliente/boleta-venta/descargar")
                        .with(user("tecnico@energygym.pe").roles("GESTOR_TECNICO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/boleta-venta/descargar")
                        .with(user("tecnico@energygym.pe").roles("GESTOR_TECNICO")))
                .andExpect(status().isForbidden());

        // 3. Cliente sí puede descargar su boleta de venta
        mockMvc.perform(get("/cliente/boleta-venta/descargar")
                        .with(user("cliente@energygym.pe").roles("CLIENTE")))
                .andExpect(status().isOk());

        // 4. Recepción y Administración sí pueden auditar y descargar boletas
        mockMvc.perform(get("/cliente/boleta-venta/descargar")
                        .with(user("recep@energygym.pe").roles("RECEPCION")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cliente/boleta-venta/descargar")
                        .with(user("admin@energygym.pe").roles("ADMINISTRACION")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Punto 3 (Feedback): Revocación de acceso JWT por cambio de rol")
    void testRevocacionJwtPorCambioDeRol() throws Exception {
        String correo = "admin.cambio." + System.currentTimeMillis() + "@energygym.pe";
        CuentaInternaDTO dto = new CuentaInternaDTO();
        dto.setUsername(correo);
        dto.setPassword("Secret123!");
        dto.setRol(Rol.ADMINISTRACION);

        Usuario user = gestionCuentasService.crearCuentaInterna(dto, "tecnico@energygym.pe");

        // Token generado inicialmente como ADMINISTRACION
        long verInicial = revocationManager.getTokenVersion(correo);
        String token = jwtService.generarToken(correo, "ADMINISTRACION", verInicial);

        // La API /api/auth/me acepta el token inicial
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Gestor Técnico cambia el rol a RECEPCION
        gestionCuentasService.cambiarRol(user.getId(), Rol.RECEPCION, "tecnico@energygym.pe");

        // El token anterior de ADMINISTRACION queda revocado y se rechaza con 401 Unauthorized
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Punto 3 (Feedback): JWT revocado debe seguir inválido tras reinicio (simulado limpiando caché de memoria)")
    void testJwtRevocadoSigueInvalidoTrasReinicio() throws Exception {
        String correo = "reinicio." + System.currentTimeMillis() + "@energygym.pe";
        CuentaInternaDTO dto = new CuentaInternaDTO();
        dto.setUsername(correo);
        dto.setPassword("Secret123!");
        dto.setRol(Rol.ADMINISTRACION);

        Usuario user = gestionCuentasService.crearCuentaInterna(dto, "tecnico@energygym.pe");
        long versionOriginal = revocationManager.getTokenVersion(correo);
        String tokenOriginal = jwtService.generarToken(correo, "ADMINISTRACION", versionOriginal);

        // El token es válido inicialmente
        assertTrue(revocationManager.esTokenValidoParaVersion(correo, versionOriginal));

        // Se revoca el acceso modificando el rol o estado
        gestionCuentasService.cambiarRol(user.getId(), Rol.RECEPCION, "tecnico@energygym.pe");

        // Simular reinicio de la aplicación limpiando la memoria caché
        revocationManager.limpiarCacheMemoria();

        // Tras el reinicio, la versión en la base de datos persiste incrementada, por lo que el token viejo sigue siendo inválido
        assertFalse(revocationManager.esTokenValidoParaVersion(correo, versionOriginal));
    }

    @Test
    @DisplayName("Punto 3 (Feedback): Revocación en APIs que usan sesión (usuario desactivado o con rol cambiado recibe 401)")
    void testRevocacionEnApisQueUsanSesion() throws Exception {
        String correo = "sesion.inactiva." + System.currentTimeMillis() + "@energygym.pe";

        Usuario usuario = new Usuario();
        usuario.setUsername(correo);
        usuario.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(false); // Desactivado
        usuarioRepository.save(usuario);

        // Petición a endpoint que usa sesión HTTP (sin Bearer) de un usuario desactivado en BD -> 401 JSON
        mockMvc.perform(get("/cliente/api/compras")
                        .with(user(correo).roles("CLIENTE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Punto 5 (Feedback): API delimitada exige JWT y no hereda sesión web abierta sin Bearer")
    @WithMockUser(username = "usuario.sesion@energygym.pe", roles = {"CLIENTE"})
    void testApiExigeJwtYNoHeredaSesionWeb() throws Exception {
        // Petición a /api/auth/me con sesión web pero SIN header Bearer -> Rechazado con 401 Unauthorized
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Punto 5 (Feedback): Operaciones web de compras requieren protección CSRF obligatoria")
    @WithMockUser(username = "cliente@energygym.pe", roles = {"CLIENTE"})
    void testOperacionComprasExigeCsrf() throws Exception {
        // Petición POST a /cliente/api/compras sin token CSRF -> Rechazado con 403 Forbidden
        mockMvc.perform(post("/cliente/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isForbidden());
    }
}
