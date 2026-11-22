package pe.edu.utp.Grupo04.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "estados_notificaciones", uniqueConstraints = @UniqueConstraint(
        name = "uk_estado_notificacion", columnNames = {"usuario_id", "aviso_clave"}))
public class EstadoNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "aviso_clave", nullable = false, length = 80)
    private String avisoClave;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private boolean archivada;
}
