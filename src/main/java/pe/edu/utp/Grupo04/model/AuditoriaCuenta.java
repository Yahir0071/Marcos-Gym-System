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

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_cuentas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String autor; // Nombre de usuario del Gestor Técnico que realiza la acción

    @Column(nullable = false)
    private String accion; // CREACION, CAMBIO_ROL, ACTIVACION, DESACTIVACION

    @Column(nullable = false)
    private String usuarioAfectado; // Correo de la cuenta afectada

    @Column(length = 500)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}
