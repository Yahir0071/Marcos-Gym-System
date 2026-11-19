package pe.edu.utp.Grupo04.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.Rol;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    private Boolean activo = true;

    @Column(name = "token_version")
    private Long tokenVersion = 1L;

    public Long getTokenVersion() {
        return tokenVersion != null ? tokenVersion : 1L;
    }

    public void setTokenVersion(Long tokenVersion) {
        this.tokenVersion = tokenVersion != null ? tokenVersion : 1L;
    }

    public Rol getRolEnum() {
        if (rol == null) return Rol.CLIENTE;
        try {
            return Rol.valueOf(rol);
        } catch (IllegalArgumentException e) {
            // Migración automática transparente si viene de una versión con ADMINISTRADOR
            if ("ADMINISTRADOR".equalsIgnoreCase(rol)) {
                return Rol.ADMINISTRACION;
            }
            return Rol.CLIENTE;
        }
    }

    public void setRolEnum(Rol rolEnum) {
        this.rol = rolEnum != null ? rolEnum.name() : Rol.CLIENTE.name();
    }

    // Métodos de conveniencia para compatibilidad con código existente
    public Rol getRol() {
        return getRolEnum();
    }

    public void setRol(Rol rolEnum) {
        setRolEnum(rolEnum);
    }
}
