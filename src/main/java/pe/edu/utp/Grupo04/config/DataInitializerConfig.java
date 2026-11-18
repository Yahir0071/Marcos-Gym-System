package pe.edu.utp.Grupo04.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.util.List;

@Configuration
public class DataInitializerConfig {

    @Bean
    public CommandLineRunner inicializarSeguridadYMigrar(UsuarioRepository usuarioRepository,
                                                        PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Migración explícita de cuentas previas que hayan quedado sin rol válido
            List<Usuario> todos = usuarioRepository.findAll();
            for (Usuario u : todos) {
                if (u.getRol() == null) {
                    u.setRol(Rol.CLIENTE);
                    usuarioRepository.save(u);
                }
            }

            // 2. Aprovisionamiento controlado de la primera cuenta de GESTOR TÉCNICO
            if (usuarioRepository.findByUsername("tecnico@energygym.pe").isEmpty()) {
                Usuario gestorTecnico = new Usuario();
                gestorTecnico.setUsername("tecnico@energygym.pe");
                gestorTecnico.setPassword(passwordEncoder.encode("Tecnico2026!"));
                gestorTecnico.setRol(Rol.GESTOR_TECNICO);
                gestorTecnico.setActivo(true);
                usuarioRepository.save(gestorTecnico);
            }

            // 3. Aprovisionamiento controlado de cuenta inicial de RECEPCIÓN
            if (usuarioRepository.findByUsername("recepcion@energygym.pe").isEmpty()) {
                Usuario recepcion = new Usuario();
                recepcion.setUsername("recepcion@energygym.pe");
                recepcion.setPassword(passwordEncoder.encode("Recepcion2026!"));
                recepcion.setRol(Rol.RECEPCION);
                recepcion.setActivo(true);
                usuarioRepository.save(recepcion);
            }

            // 4. Aprovisionamiento controlado de cuenta inicial de ADMINISTRACIÓN
            if (usuarioRepository.findByUsername("admin@energygym.pe").isEmpty()) {
                Usuario administracion = new Usuario();
                administracion.setUsername("admin@energygym.pe");
                administracion.setPassword(passwordEncoder.encode("Admin2026!"));
                administracion.setRol(Rol.ADMINISTRACION);
                administracion.setActivo(true);
                usuarioRepository.save(administracion);
            }
        };
    }
}
