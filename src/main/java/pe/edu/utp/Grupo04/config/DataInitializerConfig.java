package pe.edu.utp.Grupo04.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

@Configuration
public class DataInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);

    @Value("${app.tecnico.initial-username:}")
    private String tecnicoUsername;

    @Value("${app.tecnico.initial-password:}")
    private String tecnicoPassword;

    /**
     * Bean que ejecuta la migración SQL nativa de cuentas antiguas y actualiza la restricción CHECK:
     * 1. Asegura columna 'token_version' en usuarios si no existe.
     * 2. Elimina el check anterior 'usuarios_rol_check' si existe (creado por Hibernate en main).
     * 3. Migra los usuarios que tengan rol 'ADMINISTRADOR' o NULL a 'ADMINISTRACION'.
     * 4. Crea o actualiza 'usuarios_rol_check' para permitir ('CLIENTE', 'RECEPCION', 'ADMINISTRACION', 'GESTOR_TECNICO').
     */
    @Bean
    @Order(1)
    public CommandLineRunner migrarCuentasAntiguasSql(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                log.info("Iniciando migración SQL de cuentas y restricción usuarios_rol_check...");

                // 0. Asegurar columna token_version para persistencia de revocación JWT
                try {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_version BIGINT DEFAULT 1");
                    stmt.execute("UPDATE usuarios SET token_version = 1 WHERE token_version IS NULL");
                } catch (Exception ex) {
                    log.debug("Nota al verificar columna token_version: {}", ex.getMessage());
                }

                // 1. Eliminar restricción check antigua de PostgreSQL/H2 si existe
                try {
                    stmt.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check");
                } catch (Exception ex) {
                    log.debug("Nota al eliminar check constraint: {}", ex.getMessage());
                }

                // 2. Migrar valores anteriores de ADMINISTRADOR a ADMINISTRACION
                int actualizados = stmt.executeUpdate(
                        "UPDATE usuarios SET rol = 'ADMINISTRACION' WHERE rol = 'ADMINISTRADOR' OR rol IS NULL"
                );
                if (actualizados > 0) {
                    log.info("Migración SQL completada: {} cuenta(s) actualizada(s) de ADMINISTRADOR a ADMINISTRACION.", actualizados);
                }

                // 3. Crear nueva restricción CHECK con los 4 roles válidos
                try {
                    stmt.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('CLIENTE', 'RECEPCION', 'ADMINISTRACION', 'GESTOR_TECNICO'))");
                    log.info("Restricción usuarios_rol_check actualizada con éxito para los 4 roles.");
                } catch (Exception ex) {
                    log.debug("Nota al agregar check constraint (posiblemente ya existe o motor sin soporte): {}", ex.getMessage());
                }

            } catch (Exception e) {
                log.warn("Aviso durante migración SQL preliminar: {}", e.getMessage());
            }
        };
    }

    /**
     * Aprovisionamiento y sincronización de cuentas de prueba para cada uno de los roles del sistema.
     * Garantiza credenciales operativas válidas tanto para el formato completo (ej. admin@energygym.pe)
     * como para alias cortos (ej. admin, cliente), y vincula el perfil de socio para el rol CLIENTE.
     */
    @Bean
    @Order(2)
    public CommandLineRunner aprovisionarUsuariosDePruebaPorRol(UsuarioRepository usuarioRepository,
                                                               SocioRepository socioRepository,
                                                               PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. GESTOR TÉCNICO: Cuenta oficial y alias corto
            String tecUser = (tecnicoUsername != null && !tecnicoUsername.isBlank())
                    ? tecnicoUsername.trim().toLowerCase() : "tecnico@energygym.pe";
            String tecPass = (tecnicoPassword != null && !tecnicoPassword.isBlank())
                    ? tecnicoPassword : "Tecnico2026!";

            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, tecUser, tecPass, Rol.GESTOR_TECNICO);
            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "tecnico", tecPass, Rol.GESTOR_TECNICO);

            // 2. RECEPCIÓN: Cuenta oficial y alias corto
            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "recepcion@energygym.pe", "Recepcion2026!", Rol.RECEPCION);
            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "recepcion", "Recepcion2026!", Rol.RECEPCION);

            // 3. ADMINISTRACIÓN: Cuenta oficial y alias corto
            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "admin@energygym.pe", "Admin2026!", Rol.ADMINISTRACION);
            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "admin", "Admin2026!", Rol.ADMINISTRACION);

            // 4. CLIENTE: Cuenta oficial y alias corto
            Usuario clienteUser = provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "cliente@energygym.pe", "Cliente2026!", Rol.CLIENTE);
            provisionarOCorregirUsuario(usuarioRepository, passwordEncoder, "cliente", "Cliente2026!", Rol.CLIENTE);

            // 5. Vincular perfil Socio para cliente@energygym.pe si no existe
            if (clienteUser != null && socioRepository.findByUsuarioUsername("cliente@energygym.pe").isEmpty()) {
                try {
                    Socio socio = new Socio();
                    socio.setDni("77777777");
                    socio.setNombres("Carlos");
                    socio.setApellidos("Mendoza Rojas");
                    socio.setTelefono("987654321");
                    socio.setUsuario(clienteUser);
                    socioRepository.save(socio);
                    log.info("Perfil Socio inicial vinculado para cliente@energygym.pe (DNI: 77777777)");
                } catch (Exception e) {
                    log.debug("Nota al vincular perfil socio: {}", e.getMessage());
                }
            }
        };
    }

    private Usuario provisionarOCorregirUsuario(UsuarioRepository usuarioRepository,
                                               PasswordEncoder passwordEncoder,
                                               String username,
                                               String password,
                                               Rol rol) {
        try {
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(username.toLowerCase());
            if (userOpt.isPresent()) {
                Usuario existente = userOpt.get();
                existente.setPassword(passwordEncoder.encode(password));
                existente.setRol(rol);
                existente.setActivo(true);
                if (existente.getTokenVersion() == null) {
                    existente.setTokenVersion(1L);
                }
                Usuario guardado = usuarioRepository.save(existente);
                log.info("Credenciales sincronizadas para cuenta: {}", username);
                return guardado;
            } else {
                Usuario nuevo = new Usuario();
                nuevo.setUsername(username.toLowerCase());
                nuevo.setPassword(passwordEncoder.encode(password));
                nuevo.setRol(rol);
                nuevo.setActivo(true);
                nuevo.setTokenVersion(1L);
                Usuario guardado = usuarioRepository.save(nuevo);
                log.info("Cuenta de prueba aprovisionada exitosamente: {}", username);
                return guardado;
            }
        } catch (Exception ex) {
            log.debug("Nota al provisionar usuario {}: {}", username, ex.getMessage());
            return null;
        }
    }
}
