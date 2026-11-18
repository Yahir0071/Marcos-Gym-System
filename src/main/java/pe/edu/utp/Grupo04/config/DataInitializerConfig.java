package pe.edu.utp.Grupo04.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     * Bean que ejecuta la migración SQL nativa de cuentas antiguas ANTES de que JPA o la aplicación
     * mapeen los registros a la enumeración Java Rol. Si existían cuentas con 'ADMINISTRADOR',
     * se actualizan a 'ADMINISTRACION' para que no falle el arranque.
     */
    @Bean
    public CommandLineRunner migrarCuentasAntiguasSql(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                log.info("Verificando y ejecutando migración SQL de cuentas con rol obsoleto ADMINISTRADOR...");
                int actualizados = stmt.executeUpdate(
                        "UPDATE usuarios SET rol = 'ADMINISTRACION' WHERE rol = 'ADMINISTRADOR' OR rol IS NULL"
                );
                if (actualizados > 0) {
                    log.info("Migración SQL completada: {} cuenta(s) actualizada(s) de ADMINISTRADOR a ADMINISTRACION.", actualizados);
                }
            } catch (Exception e) {
                log.warn("Aviso durante migración SQL preliminar (puede ser base nueva o en creación): {}", e.getMessage());
            }
        };
    }

    /**
     * Aprovisionamiento controlado del primer Gestor Técnico mediante configuración (variables de entorno o properties).
     * No utiliza contraseñas fijas ni hardcodeadas.
     */
    @Bean
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
                    usuarioRepository.save(tecnico);
                    log.info("Cuenta inicial de GESTOR_TECNICO aprovisionada mediante configuración para {}", tecnicoUsername);
                }
            } else {
                log.info("No se proporcionaron app.tecnico.initial-username / app.tecnico.initial-password. Omitiendo aprovisionamiento automático.");
            }
        };
    }
}
