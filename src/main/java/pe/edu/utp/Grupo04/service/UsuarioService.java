package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;

import java.util.Optional;

public interface UsuarioService {
    Usuario registrarUsuario(String username, String rawPassword, Rol rol);
    Optional<Usuario> buscarPorUsername(String username);
    boolean existePorUsername(String username);
    void cambiarEstado(Long id, boolean activo);
}
