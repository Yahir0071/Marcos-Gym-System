package pe.edu.utp.Grupo04.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

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
     * Aprovisionamiento controlado del primer Gestor Técnico mediante configuración (variables de entorno o properties).
     * No utiliza contraseñas fijas ni hardcodeadas.
     */
    @Bean
    @Order(2)
    public CommandLineRunner aprovisionarGestorTecnicoInicial(UsuarioRepository usuarioRepository,
                                                             PasswordEncoder passwordEncoder) {
        return args -> {
            if (tecnicoUsername != null && !tecnicoUsername.isBlank() &&
                tecnicoPassword != null && !tecnicoPassword.isBlank()) {
                
                if (usuarioRepository.findByUsername(tecnicoUsername.trim().toLowerCase()).isEmpty()) {
                    Usuario tecnico = new Usuario();
                    tecnico.setUsername(tecnicoUsername.trim().toLowerCase());
                    tecnico.setPassword(passwordEncoder.encode(tecnicoPassword));
                    tecnico.setRol(Rol.GESTOR_TECNICO);
                    tecnico.setActivo(true);
                    tecnico.setTokenVersion(1L);
                    usuarioRepository.save(tecnico);
                    log.info("Cuenta inicial de GESTOR_TECNICO aprovisionada mediante configuración para {}", tecnicoUsername);
                }
            } else {
                log.info("No se proporcionaron app.tecnico.initial-username / app.tecnico.initial-password. Omitiendo aprovisionamiento automático.");
            }
        };
    }
}
