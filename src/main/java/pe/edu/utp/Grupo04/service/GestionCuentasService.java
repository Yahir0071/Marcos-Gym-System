package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.dto.CuentaInternaDTO;
import pe.edu.utp.Grupo04.model.AuditoriaCuenta;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.Rol;

import java.util.List;

public interface GestionCuentasService {
    List<Usuario> listarCuentasInternas();
    Usuario crearCuentaInterna(CuentaInternaDTO dto, String autorUsername);
    Usuario cambiarRol(Long idUsuario, Rol nuevoRol, String autorUsername);
    Usuario alternarEstadoCuenta(Long idUsuario, String autorUsername);
    List<AuditoriaCuenta> listarAuditoria();
}
