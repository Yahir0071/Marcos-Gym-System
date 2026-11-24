package pe.edu.utp.Grupo04.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.CuentaInternaDTO;
import pe.edu.utp.Grupo04.model.AuditoriaCuenta;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.AuditoriaCuentaRepository;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;
import pe.edu.utp.Grupo04.security.SecurityTokenRevocationManager;
import pe.edu.utp.Grupo04.service.GestionCuentasService;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class GestionCuentasServiceImpl implements GestionCuentasService {
    private static final List<Rol> ROLES_INTERNOS = List.of(Rol.RECEPCION, Rol.ADMINISTRACION, Rol.GESTOR_TECNICO);

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaCuentaRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityTokenRevocationManager revocationManager;

    public GestionCuentasServiceImpl(UsuarioRepository usuarioRepository,
                                     AuditoriaCuentaRepository auditoriaRepository,
                                     PasswordEncoder passwordEncoder,
                                     SecurityTokenRevocationManager revocationManager) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.revocationManager = revocationManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<Usuario> listarCuentasInternas(int pagina) {
        return usuarioRepository.findByRolInOrderByUsernameAsc(
                ROLES_INTERNOS.stream().map(Rol::name).toList(), PageRequest.of(pagina, 25));
    }

    @Override
    public Usuario crearCuentaInterna(CuentaInternaDTO dto, String autorUsername) {
        if (dto.getRol() == null || !ROLES_INTERNOS.contains(dto.getRol())) {
            throw new IllegalArgumentException("Selecciona un rol interno.");
        }
        if (dto.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña supera el tamaño permitido. Usa una más corta.");
        }
        String correo = dto.getUsername().strip().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByUsername(correo).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(correo);
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
        if (nuevoRol == null || !ROLES_INTERNOS.contains(nuevoRol)) {
            throw new IllegalArgumentException("Selecciona un rol interno.");
        }
        Usuario usuario = buscarCuentaInterna(idUsuario);

        // Evitar que el Gestor Técnico se cambie a sí mismo de rol si es el usuario logueado
        if (usuario.getUsername().equalsIgnoreCase(autorUsername) && nuevoRol != Rol.GESTOR_TECNICO) {
            throw new IllegalStateException("El Gestor Técnico no puede degradarse o alterar su propio rol en sesión activa.");
        }

        Rol rolAnterior = usuario.getRol();
        if (rolAnterior == nuevoRol) {
            return usuario;
        }
        usuario.setRol(nuevoRol);
        Usuario actualizado = usuarioRepository.save(usuario);

        // Feedback #4: Revocar inmediatamente tokens JWT y sesiones previas
        revocationManager.revocarAccesoUsuario(actualizado.getUsername());

        registrarAuditoria(autorUsername, "CAMBIO_ROL", actualizado.getUsername(),
                "Rol modificado de " + rolAnterior + " a " + nuevoRol);

        return actualizado;
    }

    @Override
    public Usuario alternarEstadoCuenta(Long idUsuario, String autorUsername) {
        Usuario usuario = buscarCuentaInterna(idUsuario);

        if (usuario.getUsername().equalsIgnoreCase(autorUsername)) {
            throw new IllegalStateException("No puede desactivar su propia cuenta en sesión activa.");
        }

        boolean nuevoEstado = usuario.getActivo() == null || !usuario.getActivo();
        usuario.setActivo(nuevoEstado);
        Usuario actualizado = usuarioRepository.save(usuario);

        // Feedback #4: Revocar inmediatamente tokens JWT y sesiones previas
        revocationManager.revocarAccesoUsuario(actualizado.getUsername());

        registrarAuditoria(autorUsername, nuevoEstado ? "ACTIVACION" : "DESACTIVACION", actualizado.getUsername(),
                "Estado de cuenta cambiado a " + (nuevoEstado ? "ACTIVO" : "INACTIVO"));

        return actualizado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaCuenta> listarAuditoria() {
        return auditoriaRepository.findTop50ByOrderByFechaHoraDescIdDesc();
    }

    private Usuario buscarCuentaInterna(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .filter(usuario -> ROLES_INTERNOS.contains(usuario.getRol()))
                .orElseThrow(() -> new IllegalArgumentException("La cuenta interna no está disponible."));
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
