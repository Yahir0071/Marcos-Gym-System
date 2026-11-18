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
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
    @DisplayName("Feedback 1: Migración SQL de cuentas obsoletas con rol ADMINISTRADOR a ADMINISTRACION")
    void testMigracionSqlCuentasAntiguas() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Insertar directamente un usuario con el texto literal 'ADMINISTRADOR' como en la BD anterior
            String userAntiguo = "antiguo." + System.currentTimeMillis() + "@energygym.pe";
            stmt.executeUpdate("INSERT INTO usuarios (username, password, rol, activo) VALUES ('" +
                    userAntiguo + "', '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'ADMINISTRADOR', true)");

            // Ejecutar la sentencia de migración explícita que corre en el arranque
            stmt.executeUpdate("UPDATE usuarios SET rol = 'ADMINISTRACION' WHERE rol = 'ADMINISTRADOR'");

            // Verificar que ahora sea ADMINISTRACION en la tabla
            try (ResultSet rs = stmt.executeQuery("SELECT rol FROM usuarios WHERE username = '" + userAntiguo + "'")) {
                assertTrue(rs.next());
                assertEquals("ADMINISTRACION", rs.getString("rol"));
            }
        }
    }

    @Test
    @DisplayName("Feedback 3: Aislamiento del Gestor Técnico: Prohibido de realizar compras ficticias")
    @WithMockUser(username = "tecnico@energygym.pe", roles = {"GESTOR_TECNICO"})
    void testGestorTecnicoProhibidoDeCompras() throws Exception {
        // Al intentar enviar una compra ficticia como GESTOR_TECNICO debe ser rechazado con 403 Forbidden
        mockMvc.perform(post("/api/compras")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[],\"buyerName\":\"Gestor Técnico\",\"buyerEmail\":\"tecnico@energygym.pe\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Feedback 4: Revocación de acceso JWT por cambio de rol")
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
    @DisplayName("Feedback 5: API delimitada exige JWT y no hereda sesión web abierta sin Bearer")
    @WithMockUser(username = "usuario.sesion@energygym.pe", roles = {"CLIENTE"})
    void testApiExigeJwtYNoHeredaSesionWeb() throws Exception {
        // Petición a /api/auth/me con sesión web pero SIN header Bearer -> Rechazado con 401 Unauthorized
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Feedback 5: Operaciones web de compras requieren protección CSRF obligatoria")
    @WithMockUser(username = "cliente@energygym.pe", roles = {"CLIENTE"})
    void testOperacionComprasExigeCsrf() throws Exception {
        // Petición POST a /api/compras sin token CSRF -> Rechazado con 403 Forbidden
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isForbidden());
    }
}
