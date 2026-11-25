package pe.edu.utp.Grupo04.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_acceso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(name = "qr_token", nullable = false, unique = true, length = 80)
    private String qrToken;

    @Column(nullable = false, length = 20)
    private String estado; // EMITIDO, PERMITIDO, DENEGADO, EXPIRADO

    @Enumerated(EnumType.STRING)
    @Column(name = "franja_horaria", length = 20)
    private FranjaHoraria franjaHoraria;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    @Column(length = 255)
    private String motivo;
}
