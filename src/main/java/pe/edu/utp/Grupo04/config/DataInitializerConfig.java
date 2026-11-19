package pe.edu.utp.Grupo04.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

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
     * 3. Migra únicamente las cuentas con rol 'ADMINISTRADOR' a 'ADMINISTRACION'.
     * 4. Crea o actualiza 'usuarios_rol_check' para permitir ('CLIENTE', 'RECEPCION', 'ADMINISTRACION', 'GESTOR_TECNICO').
     */
    @Bean
    @Profile("!prod")
    @Order(1)
    public CommandLineRunner migrarCuentasAntiguasSql(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                conn.setAutoCommit(false);
                try {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_version BIGINT DEFAULT 1");
                    stmt.execute("UPDATE usuarios SET token_version = 1 WHERE token_version IS NULL");
                    stmt.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check");
                    int actualizados = stmt.executeUpdate(
                            "UPDATE usuarios SET rol = 'ADMINISTRACION' WHERE rol = 'ADMINISTRADOR'"
                    );
                    stmt.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('CLIENTE', 'RECEPCION', 'ADMINISTRACION', 'GESTOR_TECNICO'))");
                    conn.commit();
                    log.info("Migración de roles completada. Cuentas actualizadas: {}", actualizados);
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        };
    }

    /**
     * Aprovisiona un Gestor Técnico si no hay uno activo y el usuario configurado está disponible.
     */
    @Bean
    @Order(2)
    public CommandLineRunner aprovisionarGestorTecnicoInicial(UsuarioRepository usuarioRepository,
                                                             PasswordEncoder passwordEncoder) {
        return args -> {
            if (tecnicoUsername.isBlank() || tecnicoPassword.isBlank()
                    || usuarioRepository.existsByRolAndActivoTrue(Rol.GESTOR_TECNICO.name())) {
                return;
            }

            String username = tecnicoUsername.trim().toLowerCase(Locale.ROOT);
            if (usuarioRepository.findByUsername(username).isPresent()) {
                log.warn("El nombre de usuario configurado para el Gestor Técnico ya está en uso.");
                return;
            }

            Usuario tecnico = new Usuario();
            tecnico.setUsername(username);
            tecnico.setPassword(passwordEncoder.encode(tecnicoPassword));
            tecnico.setRol(Rol.GESTOR_TECNICO);
            usuarioRepository.save(tecnico);
            log.info("Cuenta inicial de Gestor Técnico creada.");
        };
    }
}
