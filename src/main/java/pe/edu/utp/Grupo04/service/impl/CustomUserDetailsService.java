package pe.edu.utp.Grupo04.service.impl;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("El correo o usuario no puede estar vacío.");
        }

        String lookup = username.trim().toLowerCase();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(lookup);

        // Soporte de alias corto para pruebas (ej. 'admin' busca 'admin@energygym.pe')
        if (usuarioOpt.isEmpty()) {
            if ("admin".equals(lookup)) {
                usuarioOpt = usuarioRepository.findByUsername("admin@energygym.pe");
            } else if ("cliente".equals(lookup)) {
                usuarioOpt = usuarioRepository.findByUsername("cliente@energygym.pe");
            } else if ("recepcion".equals(lookup)) {
                usuarioOpt = usuarioRepository.findByUsername("recepcion@energygym.pe");
            } else if ("tecnico".equals(lookup)) {
                usuarioOpt = usuarioRepository.findByUsername("tecnico@energygym.pe");
            }
        }

        Usuario usuario = usuarioOpt
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el correo: " + username));

        if (usuario.getActivo() != null && !usuario.getActivo()) {
            throw new UsernameNotFoundException("La cuenta del usuario se encuentra inactiva.");
        }

        String roleName = "ROLE_" + usuario.getRol().name();

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
