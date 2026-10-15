package pe.edu.utp.Grupo04.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;
import pe.edu.utp.Grupo04.service.UsuarioService;

import java.util.Optional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Usuario registrarUsuario(String username, String rawPassword, Rol rol) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el correo: " + username);
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setRol(rol != null ? rol : Rol.CLIENTE);
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorUsername(String username) {
        return usuarioRepository.findByUsername(username).isPresent();
    }

    @Override
    public void cambiarEstado(Long id, boolean activo) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setActivo(activo);
            usuarioRepository.save(usuario);
        });
    }
}
