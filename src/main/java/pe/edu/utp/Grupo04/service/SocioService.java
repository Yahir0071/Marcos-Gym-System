package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Socio;

import java.util.List;
import java.util.Optional;

public interface SocioService {
    List<Socio> listarTodos();
    Optional<Socio> buscarPorId(Long id);
    Optional<Socio> buscarPorDni(String dni);
    Optional<Socio> buscarPorUsername(String username);
    Socio registrarSocio(Socio socio, String email, String rawPassword);
    Socio actualizarSocio(Long id, Socio socioDatos);
    long contarTotalSocios();
}
