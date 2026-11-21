package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.SocioActualizarDTO;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.service.UsuarioService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SocioServiceImpl implements SocioService {

    private final SocioRepository socioRepository;
    private final UsuarioService usuarioService;

    public SocioServiceImpl(SocioRepository socioRepository, UsuarioService usuarioService) {
        this.socioRepository = socioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Socio> listarTodos() {
        return socioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorId(Long id) {
        return socioRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorDni(String dni) {
        return socioRepository.findByDni(dni);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorUsername(String username) {
        return socioRepository.findByUsuarioUsername(username);
    }

    @Override
    public Socio registrarSocio(Socio socio, String email, String rawPassword) {
        if (socioRepository.findByDni(socio.getDni()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un socio registrado con el DNI: " + socio.getDni());
        }

        Usuario usuario = usuarioService.registrarUsuario(email, rawPassword, Rol.CLIENTE);
        socio.setUsuario(usuario);

        return socioRepository.save(socio);
    }

    @Override
    public Socio actualizarSocio(Long id, Socio socioDatos) {
        Socio existente = socioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado con ID: " + id));

        existente.setNombres(socioDatos.getNombres().trim());
        existente.setApellidos(socioDatos.getApellidos().trim());
        if (socioDatos.getTelefono() != null) {
            existente.setTelefono(socioDatos.getTelefono().trim());
        }

        return socioRepository.save(existente);
    }

    @Override
    public Socio actualizarSocio(Long id, SocioActualizarDTO dto) {
        Socio existente = socioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado con ID: " + id));

        existente.setNombres(dto.getNombres().trim());
        existente.setApellidos(dto.getApellidos().trim());
        if (dto.getTelefono() != null) {
            existente.setTelefono(dto.getTelefono().trim());
        }

        if (dto.getActivo() != null && existente.getUsuario() != null) {
            actualizarEstadoCuenta(existente.getUsuario(), dto.getActivo());
        }

        return socioRepository.save(existente);
    }

    @Override
    public void cambiarEstado(Long id, boolean activo) {
        Socio existente = socioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado con ID: " + id));
        if (existente.getUsuario() == null) {
            throw new IllegalStateException("Este socio no tiene una cuenta de acceso.");
        }
        actualizarEstadoCuenta(existente.getUsuario(), activo);
        socioRepository.save(existente);
    }

    private void actualizarEstadoCuenta(Usuario usuario, boolean activo) {
        if (usuario.getRol() != Rol.CLIENTE) {
            throw new IllegalArgumentException("Las cuentas internas se gestionan desde Cuentas y roles.");
        }
        if (!Boolean.valueOf(activo).equals(usuario.getActivo())) {
            usuario.setActivo(activo);
            usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long contarTotalSocios() {
        return socioRepository.count();
    }
}
