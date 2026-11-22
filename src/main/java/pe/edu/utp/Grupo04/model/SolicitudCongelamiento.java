package pe.edu.utp.Grupo04.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_congelamiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCongelamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "socio_id")
    private Socio socio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "membresia_id")
    private Membresia membresia;

    @Column(nullable = false)
    private Integer diasSolicitados;

    @Column(nullable = false)
    private LocalDate fechaInicioSolicitada;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudCongelamiento estado = EstadoSolicitudCongelamiento.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaResolucion;

    private String resueltoPor;
}
