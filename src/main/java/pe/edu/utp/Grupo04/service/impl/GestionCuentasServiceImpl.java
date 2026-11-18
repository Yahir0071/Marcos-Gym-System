package pe.edu.utp.Grupo04.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.CuentaInternaDTO;
import pe.edu.utp.Grupo04.model.AuditoriaCuenta;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.AuditoriaCuentaRepository;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;
import pe.edu.utp.Grupo04.service.GestionCuentasService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class GestionCuentasServiceImpl implements GestionCuentasService {

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaCuentaRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;

    public GestionCuentasServiceImpl(UsuarioRepository usuarioRepository,
                                     AuditoriaCuentaRepository auditoriaRepository,
                                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarCuentasInternas() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == Rol.RECEPCION || u.getRol() == Rol.ADMINISTRACION || u.getRol() == Rol.GESTOR_TECNICO)
                .toList();
    }

    @Override
    public Usuario crearCuentaInterna(CuentaInternaDTO dto, String autorUsername) {
        if (dto.getRol() == Rol.CLIENTE) {
            throw new IllegalArgumentException("No se pueden crear cuentas de CLIENTE desde el panel de Gestión Técnica.");
        }
        if (usuarioRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el correo: " + dto.getUsername());
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(dto.getUsername().trim());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(dto.getRol());
        usuario.setActivo(true);
        Usuario guardado = usuarioRepository.save(usuario);

        registrarAuditoria(autorUsername, "CREACION", guardado.getUsername(),
                "Cuenta creada con rol " + guardado.getRol());

        return guardado;
    }

    @Override
    public Usuario cambiarRol(Long idUsuario, Rol nuevoRol, String autorUsername) {
        if (nuevoRol == Rol.CLIENTE) {
            throw new IllegalArgumentException("No se puede asignar el rol CLIENTE a una cuenta interna.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + idUsuario));

        // Evitar que el Gestor Técnico se cambie a sí mismo de rol si es el usuario logueado
        if (usuario.getUsername().equalsIgnoreCase(autorUsername) && nuevoRol != Rol.GESTOR_TECNICO) {
            throw new IllegalStateException("El Gestor Técnico no puede degradarse o alterar su propio rol en sesión activa.");
        }

        Rol rolAnterior = usuario.getRol();
        usuario.setRol(nuevoRol);
        Usuario actualizado = usuarioRepository.save(usuario);

        registrarAuditoria(autorUsername, "CAMBIO_ROL", actualizado.getUsername(),
                "Rol modificado de " + rolAnterior + " a " + nuevoRol);

        return actualizado;
    }

    @Override
    public Usuario alternarEstadoCuenta(Long idUsuario, String autorUsername) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + idUsuario));

        if (usuario.getUsername().equalsIgnoreCase(autorUsername)) {
            throw new IllegalStateException("No puede desactivar su propia cuenta en sesión activa.");
        }

        boolean nuevoEstado = usuario.getActivo() == null || !usuario.getActivo();
        usuario.setActivo(nuevoEstado);
        Usuario actualizado = usuarioRepository.save(usuario);

        registrarAuditoria(autorUsername, nuevoEstado ? "ACTIVACION" : "DESACTIVACION", actualizado.getUsername(),
                "Estado de cuenta cambiado a " + (nuevoEstado ? "ACTIVO" : "INACTIVO"));

        return actualizado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaCuenta> listarAuditoria() {
        return auditoriaRepository.findAllByOrderByFechaHoraDesc();
    }

    private void registrarAuditoria(String autor, String accion, String usuarioAfectado, String detalle) {
        AuditoriaCuenta audit = new AuditoriaCuenta();
        audit.setAutor(autor != null ? autor : "SISTEMA");
        audit.setAccion(accion);
        audit.setUsuarioAfectado(usuarioAfectado);
        audit.setDetalle(detalle);
        audit.setFechaHora(LocalDateTime.now());
        auditoriaRepository.save(audit);
    }
}
