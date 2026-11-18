package pe.edu.utp.Grupo04.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.utp.Grupo04.dto.CuentaInternaDTO;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.security.jwt.JwtService;
import pe.edu.utp.Grupo04.service.GestionCuentasService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityAlonsoTests {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GestionCuentasService gestionCuentasService;

    @Test
    @DisplayName("JWT: Emisión, lectura de claims y validación de firma correcta")
    void testEmisionYValidacionJwt() {
        String token = jwtService.generarToken("alonso@energygym.pe", "GESTOR_TECNICO");
        assertNotNull(token);
        assertTrue(jwtService.validarToken(token));
        assertEquals("alonso@energygym.pe", jwtService.extraerUsername(token));
        assertEquals("GESTOR_TECNICO", jwtService.extraerRol(token));
    }

    @Test
    @DisplayName("JWT: Detección y rechazo de token alterado")
    void testTokenAlteradoRechazado() {
        String token = jwtService.generarToken("usuario@energygym.pe", "CLIENTE");
        String tokenAlterado = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(jwtService.validarToken(tokenAlterado));
    }

    @Test
    @DisplayName("JWT: Detección y rechazo de token vencido")
    void testTokenVencidoRechazado() {
        // Generar token con expiración en el pasado
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", "CLIENTE");
        Date pasado = new Date(System.currentTimeMillis() - 10000);

        String tokenVencido = io.jsonwebtoken.Jwts.builder()
                .claims(claims)
                .subject("vencido@energygym.pe")
                .issuedAt(new Date(System.currentTimeMillis() - 20000))
                .expiration(pasado)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("MarcosGymSecretKeyParaPruebasUnitariasYSeguridad2026Alonso".getBytes()))
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
}
